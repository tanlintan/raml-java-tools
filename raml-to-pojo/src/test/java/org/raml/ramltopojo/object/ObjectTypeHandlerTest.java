package org.raml.ramltopojo.object;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.raml.ramltopojo.*;
import org.raml.ramltopojo.plugin.PluginManager;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.StringTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URL;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.raml.ramltopojo.RamlLoader.findTypes;
import static org.raml.testutils.matchers.FieldSpecMatchers.*;
import static org.raml.testutils.matchers.MethodSpecMatchers.*;
import static org.raml.testutils.matchers.ParameterSpecMatchers.type;
import static org.raml.testutils.matchers.TypeNameMatcher.typeName;
import static org.raml.testutils.matchers.TypeSpecMatchers.*;

/**
 * Created. There, you have it.
 */
public class ObjectTypeHandlerTest {

    @Test
    public void simplest() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("simplest-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(new GenerationContextImpl(api));

        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(contains(
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getAge")), returnType(equalTo(ClassName.INT))),
                        allOf(methodName(equalTo("setAge")), parameters(contains(type(equalTo(ClassName.INT)))))
                ))
        )));

        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(contains(
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("age")), fieldType(equalTo(ClassName.INT)))
                )),
                methods(contains(
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class))), codeContent(equalTo("return this.name;\n"))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))), codeContent(equalTo("this.name = name;\n"))),
                        allOf(methodName(equalTo("getAge")), returnType(equalTo(ClassName.INT))),
                        allOf(methodName(equalTo("setAge")), parameters(contains(type(equalTo(ClassName.INT)))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Foo"))))
                ))
        )));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());
    }

    @Test
    public void simplestContainingSimpleArray() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("simplest-containing-simple-array.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(new GenerationContextImpl(api));
        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());

        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(contains(
                        allOf(methodName(equalTo("getNames")), returnType(equalTo(ParameterizedTypeName.get(List.class, String.class)))),
                        allOf(methodName(equalTo("setNames")), parameters(contains(type(equalTo(ParameterizedTypeName.get(List.class, String.class)))))),
                        allOf(methodName(equalTo("getAges")), returnType(equalTo(ParameterizedTypeName.get(List.class, Integer.class)))),
                        allOf(methodName(equalTo("setAges")), parameters(contains(type(equalTo(ParameterizedTypeName.get(List.class, Integer.class))))))
                ))
        )));


        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(contains(
                        allOf(fieldName(equalTo("names")), fieldType(equalTo(ParameterizedTypeName.get(List.class, String.class)))),
                        allOf(fieldName(equalTo("ages")), fieldType(equalTo(ParameterizedTypeName.get(List.class, Integer.class))))
                )),
                methods(contains(
                        allOf(methodName(equalTo("getNames")), returnType(equalTo(ParameterizedTypeName.get(List.class, String.class)))),
                        allOf(methodName(equalTo("setNames")), parameters(contains(type(equalTo(ParameterizedTypeName.get(List.class, String.class)))))),
                        allOf(methodName(equalTo("getAges")), returnType(equalTo(ParameterizedTypeName.get(List.class, Integer.class)))),
                        allOf(methodName(equalTo("setAges")), parameters(contains(type(equalTo(ParameterizedTypeName.get(List.class, Integer.class))))))
                ))
        )));
    }

    @Test
    public void usingComposedTypes() throws Exception {

        final Api api = RamlLoader.load(this.getClass().getResourceAsStream("using-composed-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(createGenerationContext(api));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());

        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(contains(
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get("", "Composed")))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get("", "Composed"))))))
                ))
        )));

        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(contains(
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get("", "Composed"))))
                )),
                methods(contains(
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get("", "Composed")))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get("", "Composed")))))))
                ))
        ));
    }

    @Test
    public void simpleInheritance() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("inherited-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(createGenerationContext(api));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());

        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getAge")), returnType(equalTo(ClassName.INT))),
                        allOf(methodName(equalTo("setAge")), parameters(contains(type(equalTo(ClassName.INT))))),
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Inherited"))))

                )))));

        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(containsInAnyOrder(
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("age")), fieldType(equalTo(ClassName.INT)))
                )),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getAge")), returnType(equalTo(ClassName.INT))),
                        allOf(methodName(equalTo("setAge")), parameters(contains(type(equalTo(ClassName.INT)))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Foo"))))
                ))
        )));
    }

    @Test
    public void inheritanceWithDiscriminator() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("inheritance-with-discriminator-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(createGenerationContext(api));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());


        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getKind")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("getRight")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setRight")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Once"))))

                )))));

        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(containsInAnyOrder(
                        allOf(fieldName(equalTo("kind")), fieldType(equalTo(ClassName.get(String.class))), initializer(equalTo("\"foo\""))),
                        allOf(fieldName(equalTo("right")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get(String.class))))
                )),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getKind")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("getRight")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setRight")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Foo"))))
                ))
        )));

    }

    @Test
    public void inheritanceWithDiscriminatorAndValue() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("inheritance-with-discriminatorvalue-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(createGenerationContext(api));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());


        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(containsInAnyOrder(
                        allOf(fieldName(equalTo("kind")), fieldType(equalTo(ClassName.get(String.class))), initializer(equalTo("\"myOwnValue\""))),
                        allOf(fieldName(equalTo("right")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get(String.class))))
                ))
        )));


    }

    @Test
    public void multipleInheritance() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("multiple-inheritance-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(RamlLoader.findTypes("foo", api.types()));

        CreationResult r = handler.create(createGenerationContext(api));

        System.err.println(r.getInterface().toString());
        System.err.println(r.getImplementation().toString());

        assertThat(r.getInterface(), is(allOf(
                name(equalTo("Foo")),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getLeft")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setLeft")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getRight")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setRight")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))))
                )),
                superInterfaces(contains(
                                typeName(equalTo(ClassName.get("", "Once"))),
                                typeName(equalTo(ClassName.get("", "Twice")))
                        )

                ))));

        assertThat(r.getImplementation().get(), is(allOf(
                name(equalTo("FooImpl")),
                fields(containsInAnyOrder(
                        allOf(fieldName(equalTo("left")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("right")), fieldType(equalTo(ClassName.get(String.class)))),
                        allOf(fieldName(equalTo("name")), fieldType(equalTo(ClassName.get(String.class))))
                )),
                methods(containsInAnyOrder(
                        allOf(methodName(equalTo("getLeft")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setLeft")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getRight")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setRight")), parameters(contains(type(equalTo(ClassName.get(String.class)))))),
                        allOf(methodName(equalTo("getName")), returnType(equalTo(ClassName.get(String.class)))),
                        allOf(methodName(equalTo("setName")), parameters(contains(type(equalTo(ClassName.get(String.class))))))
                )),
                superInterfaces(contains(
                        allOf(typeName(equalTo(ClassName.get("", "Foo"))))
                ))
        )));
    }

    @Test
    public void simplestInternal() throws Exception {

        Api api = RamlLoader.load(this.getClass().getResourceAsStream("inline-type.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(findTypes("foo", api.types()));

        CreationResult r = handler.create(new GenerationContextImpl(api));

        assertThat(r.getInternalTypeForProperty("inside").getInterface(), name(equalTo("Inside")));
        assertThat(r.getInternalTypeForProperty("inside").getImplementation().get(), name(equalTo("InsideImpl")));
    }

    @Test
    public void pluginCalled() throws Exception {

        final ObjectTypeHandlerPlugin mockPlugin = mock(ObjectTypeHandlerPlugin.class);
        when(mockPlugin.classCreated(ArgumentMatchers.any(ObjectTypeDeclaration.class), ArgumentMatchers.any(TypeSpec.Builder.class), eq(EventType.INTERFACE))).thenAnswer(new Answer<TypeSpec.Builder>() {
            @Override
            public TypeSpec.Builder answer(InvocationOnMock invocation) throws Throwable {
                return (TypeSpec.Builder) invocation.getArguments()[1];
            }
        });
        when(mockPlugin.getterBuilt(ArgumentMatchers.any(ObjectTypeDeclaration.class), ArgumentMatchers.any(MethodSpec.Builder.class), eq(EventType.INTERFACE))).thenAnswer(new Answer<MethodSpec.Builder>() {
            @Override
            public MethodSpec.Builder answer(InvocationOnMock invocation) throws Throwable {
                return (MethodSpec.Builder) invocation.getArguments()[1];
            }
        });
        when(mockPlugin.setterBuilt(ArgumentMatchers.any(ObjectTypeDeclaration.class), ArgumentMatchers.any(MethodSpec.Builder.class), eq(EventType.INTERFACE))).thenAnswer(new Answer<MethodSpec.Builder>() {
            @Override
            public MethodSpec.Builder answer(InvocationOnMock invocation) throws Throwable {
                return (MethodSpec.Builder) invocation.getArguments()[1];
            }
        });


        Api api = RamlLoader.load(this.getClass().getResourceAsStream("plugin-test.raml"), ".");
        ObjectTypeHandler handler = new ObjectTypeHandler(findTypes("foo", api.types()));

        CreationResult r = handler.create(new GenerationContextImpl(api) {
            @Override
            public ObjectTypeHandlerPlugin pluginsForObjects(TypeDeclaration... typeDeclarations) {
                return mockPlugin;
            }
        });

        assertNotNull(r);
        assertFalse(r.getImplementation().isPresent());
        verify(mockPlugin, times(1)).classCreated(ArgumentMatchers.any(ObjectTypeDeclaration.class), ArgumentMatchers.any(TypeSpec.Builder.class), eq(EventType.INTERFACE));
        verify(mockPlugin, times(2)).getterBuilt(ArgumentMatchers.any(StringTypeDeclaration.class), ArgumentMatchers.any(MethodSpec.Builder.class), eq(EventType.INTERFACE));
        verify(mockPlugin, times(2)).setterBuilt(ArgumentMatchers.any(StringTypeDeclaration.class), ArgumentMatchers.any(MethodSpec.Builder.class), eq(EventType.INTERFACE));
    }

    @Test
    public void checkAnnotations() throws Exception {

        URL url = this.getClass().getResource("plugin-invocation.raml");
        Api api = RamlLoader.load(url.openStream(), new File(url.getFile()).getAbsolutePath());
        ObjectTypeHandler handler = new ObjectTypeHandler(findTypes("foo", api.types()));

        CreationResult r = handler.create(new GenerationContextImpl(PluginManager.createPluginManager("org/raml/ramltopojo/object/simple-plugin.properties"), api, TypeFetchers.NULL_FETCHER, "hello"));

        assertNotNull(r);
        assertTrue(r.getInterface().annotations.size() == 1);
        assertEquals("@java.lang.Deprecated", r.getInterface().annotations.get(0).toString());
        assertTrue(r.getImplementation().get().annotations.size() == 1);
        assertEquals("@java.lang.Deprecated", r.getImplementation().get().annotations.get(0).toString());
    }

    protected GenerationContextImpl createGenerationContext(final Api api) {
        return new GenerationContextImpl(PluginManager.NULL, api, new TypeFetcher() {
            @Override
            public TypeDeclaration fetchType(Api api, final String name) throws GenerationException {
                return FluentIterable.from(api.types()).firstMatch(new Predicate<TypeDeclaration>() {
                    @Override
                    public boolean apply(@Nullable TypeDeclaration input) {
                        return input.name().equals(name);
                    }
                }).or(new Supplier<TypeDeclaration>() {
                    @Override
                    public TypeDeclaration get() {
                        throw new GenerationException("type " + name + " not found");
                    }
                });
            }
        }, "pojo.pack");
    }


}