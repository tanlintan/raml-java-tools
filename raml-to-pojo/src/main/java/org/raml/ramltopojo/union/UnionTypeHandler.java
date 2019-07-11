package org.raml.ramltopojo.union;

import amf.client.model.domain.NilShape;
import amf.client.model.domain.Shape;
import amf.client.model.domain.UnionShape;
import com.google.common.base.Function;
import com.squareup.javapoet.*;
import org.raml.ramltopojo.*;
import org.raml.ramltopojo.extensions.UnionPluginContext;
import org.raml.ramltopojo.extensions.UnionPluginContextImpl;
import org.raml.ramltopojo.extensions.UnionTypeHandlerPlugin;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.NullTypeDeclaration;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created. There, you have it.
 */
public class UnionTypeHandler implements TypeHandler {

    private final String name;
    private final UnionShape union;
    public static final ClassName NULL_CLASS = ClassName.get(Object.class);

    public UnionTypeHandler(String name, UnionShape union) {
        this.name = name;
        this.union = union;
    }

    @Override
    public ClassName javaClassName(GenerationContext generationContext, EventType type) {

        UnionPluginContext context = new UnionPluginContextImpl(generationContext, null);

        UnionTypeHandlerPlugin plugin = generationContext.pluginsForUnions(Utils.allParents(union, new ArrayList<>()).toArray(new Shape[0]));
        ClassName className;
        if ( type == EventType.IMPLEMENTATION ) {
            className = generationContext.buildDefaultClassName(Names.typeName(name, "Impl"), EventType.IMPLEMENTATION);
        } else {

            className = generationContext.buildDefaultClassName(Names.typeName(name), EventType.INTERFACE);
        }

        return plugin.className(context, union, className, type);
    }

    @Override
    public TypeName javaClassReference(GenerationContext generationContext, EventType type) {
        return javaClassName(generationContext, type);
    }

    @Override
    public Optional<CreationResult> create(GenerationContext generationContext, CreationResult preCreationResult) {

        UnionPluginContext context = new UnionPluginContextImpl(generationContext, preCreationResult);

        ClassName interfaceName = preCreationResult.getJavaName(EventType.INTERFACE);

        TypeSpec.Builder interf = getDeclaration(generationContext, context, preCreationResult);
        TypeSpec.Builder impl = getImplementation(interfaceName, generationContext, context, preCreationResult);

        if ( interf == null ) {

            return Optional.empty();
        } else {
            return Optional.of(preCreationResult.withInterface(interf.build()).withImplementation(impl.build()));
        }
    }

    private TypeSpec.Builder getImplementation(ClassName interfaceName, GenerationContext generationContext, UnionPluginContext context, CreationResult preCreationResult) {
        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(preCreationResult.getJavaName(EventType.IMPLEMENTATION))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceName);

        FieldSpec.Builder anyType = FieldSpec.builder(Object.class, "anyType", Modifier.PRIVATE);
        anyType = generationContext.pluginsForUnions(union).anyFieldCreated(context, union, typeSpec, anyType, EventType.IMPLEMENTATION);
        if ( anyType == null ) {

            return typeSpec;
        }

        boolean hasNullType = union.anyOf().stream().anyMatch(input -> input instanceof NilShape);

        typeSpec.addField(anyType.build());
        typeSpec.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(hasNullType ? Modifier.PUBLIC: Modifier.PRIVATE).addStatement("this.anyType = null")

                        .build());

        for (Shape unitedType : union.anyOf()) {

            TypeName typeName =  unitedType instanceof NullTypeDeclaration ? NULL_CLASS : findType(unitedType.name().value(), unitedType, generationContext).box();
            String shortened = shorten(typeName);

            String fieldName = Names.methodName(unitedType.name().value());
            if ( typeName == NULL_CLASS ) {

                typeSpec
                        .addMethod(
                                MethodSpec
                                        .methodBuilder("getNil")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(typeName)
                                        .addStatement(
                                                "if ( !(anyType == null)) throw new $T(\"fetching wrong type out of the union: NullType should be null\")",
                                                IllegalStateException.class)
                                        .addStatement("return null").build())
                        .addMethod(
                                MethodSpec.methodBuilder("isNil")
                                        .addStatement("return anyType == null")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(TypeName.BOOLEAN).build()
                        ).build();

            } else {
                typeSpec
                        .addMethod(
                                MethodSpec.constructorBuilder()
                                        .addParameter(ParameterSpec.builder(typeName, fieldName).build())
                                        .addModifiers(Modifier.PUBLIC).addStatement("this.anyType = $L", fieldName)
                                        .build())
                        .addMethod(
                                MethodSpec
                                        .methodBuilder(Names.methodName("get", shortened))
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(typeName)
                                        .addStatement(
                                                "if ( !(anyType instanceof  $T)) throw new $T(\"fetching wrong type out of the union: $L\")",
                                                typeName, IllegalStateException.class, typeName)
                                        .addStatement("return ($T) anyType", typeName).build())
                        .addMethod(
                                MethodSpec.methodBuilder(Names.methodName("is", shortened))
                                        .addStatement("return anyType instanceof $T", typeName)
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(TypeName.BOOLEAN).build()
                        ).build();
            }
        }

        typeSpec = generationContext.pluginsForUnions(union).classCreated(context, union, typeSpec, EventType.IMPLEMENTATION);
        if ( typeSpec == null ) {
            return null;
        }

        return typeSpec;
    }

    private TypeSpec.Builder getDeclaration(final GenerationContext generationContext, UnionPluginContext context, CreationResult preCreationResult) {

        TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(preCreationResult.getJavaName(EventType.INTERFACE))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        List<TypeName> names = union.anyOf().stream().map(new Function<Shape, TypeName>() {
            @Nullable
            @Override
            public TypeName apply(@Nullable Shape unitedType) {

                if (unitedType instanceof NullTypeDeclaration) {
                    return NULL_CLASS;
                } else {
                    return findType(unitedType.name().value(), unitedType, generationContext).box();
                }
            }
        }).collect(Collectors.toList());

        typeSpec = generationContext.pluginsForUnions(union).classCreated(context, union, typeSpec, EventType.INTERFACE);
        if ( typeSpec == null ) {
            return null;
        }


        for (TypeName unitedType : names) {

            if ( unitedType instanceof ArrayTypeDeclaration ) {

                throw new GenerationException("ramltopojo currently does not support arrays in unions");
            }

            if ( unitedType == NULL_CLASS ) {

                typeSpec
                        .addMethod(
                                MethodSpec
                                        .methodBuilder("getNil")
                                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                        .returns(unitedType).build())
                        .addMethod(
                                MethodSpec.methodBuilder("isNil")
                                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                        .returns(TypeName.BOOLEAN).build()
                        );
            } else {

                String shortened = shorten(unitedType);

                typeSpec
                        .addMethod(
                                MethodSpec
                                        .methodBuilder(Names.methodName("get", shortened))
                                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                        .returns(unitedType).build())
                        .addMethod(
                                MethodSpec.methodBuilder(Names.methodName("is", shortened))
                                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                        .returns(TypeName.BOOLEAN).build()
                        );
            }
        }
        return typeSpec;
    }

    private String shorten(TypeName typeName) {

        if ( ! (typeName instanceof ClassName) ) {

            throw new GenerationException(typeName + toString() +  " cannot be shortened reasonably");
        } else {

            return ((ClassName)typeName).simpleName();
        }
    }

    private TypeName findType(String typeName, Shape type, GenerationContext generationContext) {

        return TypeDeclarationType.calculateTypeName(typeName,type, generationContext, EventType.INTERFACE);
    }
}
