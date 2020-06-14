package org.raml.builder;

import amf.client.model.domain.AnyShape;
import amf.client.model.domain.DomainElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.raml.builder.NodeBuilders.property;

/**
 * Created. There, you have it.
 */
public abstract class TypeShapeBuilder<N extends AnyShape, B extends TypeShapeBuilder<N, ?>> extends SomeShapeBuilder<TypeShapeBuilder> implements NodeBuilder, AnnotableBuilder<TypeShapeBuilder> {

    private List<ExamplesBuilder> examples = new ArrayList<>();
    private List<AnnotationBuilder> annotations = new ArrayList<>();
    private List<FacetBuilder> facets = new ArrayList<>();


    private String description;

    private ExamplesBuilder example;

    private TypeShapeBuilder arrayItems;


    static public TypeShapeBuilder simpleType(String type) {

        return new NodeShapeBuilder(type);
    }

    static public ArrayShapeBuilder arrayOf(TypeShapeBuilder builder) {

        return new ArrayShapeBuilder(builder);
    }

    /* for enums */
    static public ScalarShapeBuilder enumeratedType() {

        // todo not hello
        return new ScalarShapeBuilder("hello");
    }

    static public NodeShapeBuilder inheritingObject(String... types) {

        return new NodeShapeBuilder(types);
    }

    @Override
    public TypeShapeBuilder withAnnotations(AnnotationBuilder... builders) {

        this.annotations.addAll(Arrays.asList(builders));
        return this;
    }


    public TypeShapeBuilder withExamples(ExamplesBuilder... properties) {

        this.example = null;
        this.examples.addAll(Arrays.asList(properties));
        return this;
    }

    public TypeShapeBuilder withExample(ExamplesBuilder example) {

        this.examples.clear();
        this.example = example;
        return this;
    }

    public TypeShapeBuilder withFacets(FacetBuilder... facetBuilders) {

        this.facets.addAll(Arrays.asList(facetBuilders));
        return this;
    }

    public TypeShapeBuilder description(String description) {

        this.description = description;
        return this;
    }


    public void commonNodeInfo(AnyShape node) {


        if ( ! facets.isEmpty() ) {


//            KeyValueNodeImpl kvn = new KeyValueNodeImpl(new StringNodeImpl("facets"), new ObjectNodeImpl());
//            for (FacetBuilder facetBuilder : facets) {
//                kvn.getValue().addChild(facetBuilder.buildNode());
//            }
//
//            node.addChild(kvn);
        }

        if ( description != null ) {

            node.withDescription(description);
        }


        if ( ! annotations.isEmpty() ) {

//            for (AnnotationBuilder annotation : annotations) {
//                node.addChild(annotation.buildNode());
//            }
        }


        if ( ! examples.isEmpty() ) {

            node.withExamples(examples.stream().map(ExamplesBuilder::buildNode).collect(Collectors.toList()));
        }

        if ( example != null ) {

            node.withExamples(Collections.singletonList(example.buildNode()));
        }
    }

    public TypeShapeBuilder withFormat(PropertyValueBuilder format) {
        return null;
    }
}
