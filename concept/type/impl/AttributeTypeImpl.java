/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package hypergraph.concept.type.impl;

import hypergraph.common.exception.Error;
import hypergraph.common.exception.HypergraphException;
import hypergraph.common.iterator.Iterators;
import hypergraph.concept.thing.Attribute;
import hypergraph.concept.thing.impl.AttributeImpl;
import hypergraph.concept.type.AttributeType;
import hypergraph.concept.type.RoleType;
import hypergraph.graph.TypeGraph;
import hypergraph.graph.util.Schema;
import hypergraph.graph.vertex.AttributeVertex;
import hypergraph.graph.vertex.TypeVertex;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static hypergraph.common.exception.Error.ConceptRead.INVALID_CONCEPT_CASTING;
import static hypergraph.common.exception.Error.Internal.UNRECOGNISED_VALUE;
import static hypergraph.common.exception.Error.TypeRead.TYPE_ROOT_MISMATCH;
import static hypergraph.common.exception.Error.TypeWrite.ATTRIBUTE_SUPERTYPE_NOT_ABSTRACT;
import static hypergraph.common.exception.Error.TypeWrite.ATTRIBUTE_SUPERTYPE_VALUE_TYPE;
import static hypergraph.common.exception.Error.TypeWrite.ROOT_TYPE_MUTATION;
import static hypergraph.common.exception.Error.TypeWrite.SUPERTYPE_SELF;
import static hypergraph.common.iterator.Iterators.apply;
import static hypergraph.common.iterator.Iterators.stream;

public abstract class AttributeTypeImpl extends ThingTypeImpl implements AttributeType {

    private AttributeTypeImpl(TypeVertex vertex) {
        super(vertex);
        if (vertex.schema() != Schema.Vertex.Type.ATTRIBUTE_TYPE) {
            throw new HypergraphException(TYPE_ROOT_MISMATCH.format(
                    vertex.label(),
                    Schema.Vertex.Type.ATTRIBUTE_TYPE.root().label(),
                    vertex.schema().root().label()
            ));
        }
    }

    private AttributeTypeImpl(TypeGraph graph, java.lang.String label, Class<?> valueType) {
        super(graph, label, Schema.Vertex.Type.ATTRIBUTE_TYPE);
        vertex.valueType(Schema.ValueType.of(valueType));
    }

    public static AttributeTypeImpl of(TypeVertex vertex) {
        switch (vertex.valueType()) {
            case OBJECT:
                return new AttributeTypeImpl.Root(vertex);
            case BOOLEAN:
                return AttributeTypeImpl.Boolean.of(vertex);
            case LONG:
                return AttributeTypeImpl.Long.of(vertex);
            case DOUBLE:
                return AttributeTypeImpl.Double.of(vertex);
            case STRING:
                return AttributeTypeImpl.String.of(vertex);
            case DATETIME:
                return AttributeTypeImpl.DateTime.of(vertex);
            default:
                throw new HypergraphException(UNRECOGNISED_VALUE);
        }
    }

    @Nullable
    @Override
    public abstract AttributeTypeImpl sup();

    @Override
    public abstract Stream<? extends AttributeTypeImpl> sups();

    @Override
    public abstract Stream<? extends AttributeTypeImpl> subs();

    @Override
    public abstract Stream<? extends AttributeImpl<?>> instances();

    Iterator<TypeVertex> subTypeVertices(Schema.ValueType valueType) {
        return Iterators.tree(vertex, v -> Iterators.filter(v.ins().edge(Schema.Edge.Type.SUB).from(),
                                                            sv -> sv.valueType().equals(valueType)));
    }

    @Override
    public void sup(AttributeType superType) {
        if (!superType.isRoot() && !this.valueType().equals(superType.valueType())) {
            throw new HypergraphException(ATTRIBUTE_SUPERTYPE_VALUE_TYPE.format(
                    label(), valueType().getSimpleName(), superType.label(), superType.valueType().getSimpleName()
            ));
        } else if (this.equals(superType)) {
            throw new HypergraphException(SUPERTYPE_SELF.format(label()));
        } else if (!superType.isAbstract()) {
            throw new HypergraphException(ATTRIBUTE_SUPERTYPE_NOT_ABSTRACT.format(superType.label()));
        }
        vertex.outs().edge(Schema.Edge.Type.SUB, sup().vertex).delete();
        vertex.outs().put(Schema.Edge.Type.SUB, ((AttributeTypeImpl) superType).vertex);
    }

    @Override
    public boolean isKeyable() {
        return vertex.valueType().isKeyable();
    }

    @Override
    public Class<?> valueType() {
        return Object.class;
    }

    @Override
    public List<HypergraphException> validate() {
        return super.validate();
        // TODO: Add any validation that would apply to all AttributeTypes here
    }

    @Override
    public AttributeTypeImpl.Root asObject() {
        if (this.valueType().equals(java.lang.Object.class)) return new AttributeTypeImpl.Root(this.vertex);
        else throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.class.getCanonicalName()));
    }

    @Override
    public AttributeTypeImpl.Boolean asBoolean() {
        throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.Boolean.class.getCanonicalName()));
    }

    @Override
    public AttributeTypeImpl.Long asLong() {
        throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.Long.class.getCanonicalName()));
    }

    @Override
    public AttributeTypeImpl.Double asDouble() {
        throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.Double.class.getCanonicalName()));
    }

    @Override
    public AttributeTypeImpl.String asString() {
        throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.String.class.getCanonicalName()));
    }

    @Override
    public AttributeTypeImpl.DateTime asDateTime() {
        throw new HypergraphException(INVALID_CONCEPT_CASTING.format(AttributeType.DateTime.class.getCanonicalName()));
    }

    @Override
    public boolean equals(java.lang.Object object) {
        if (this == object) return true;
        if (!(object instanceof AttributeTypeImpl)) return false;
        // We do the above, as opposed to checking if (object == null || getClass() != object.getClass())
        // because it is possible to compare a attribute root types wrapped in different type classes
        // such as: root type wrapped in AttributeTypeImpl.Root and as in AttributeType.Boolean.Root

        AttributeTypeImpl that = (AttributeTypeImpl) object;
        return this.vertex.equals(that.vertex);
    }

    private static class Root extends AttributeTypeImpl {

        private Root(TypeVertex vertex) {
            super(vertex);
            assert vertex.valueType().equals(Schema.ValueType.OBJECT);
            assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
        }

        public Class<java.lang.Object> valueType() { return java.lang.Object.class; }

        @Override
        public AttributeTypeImpl.Root asObject() { return this; }

        @Override
        public AttributeTypeImpl.Boolean asBoolean() { return AttributeTypeImpl.Boolean.of(this.vertex); }

        @Override
        public AttributeTypeImpl.Long asLong() { return AttributeTypeImpl.Long.of(this.vertex); }

        @Override
        public AttributeTypeImpl.Double asDouble() { return AttributeTypeImpl.Double.of(this.vertex); }

        @Override
        public AttributeTypeImpl.String asString() { return AttributeTypeImpl.String.of(this.vertex); }

        @Override
        public AttributeTypeImpl.DateTime asDateTime() { return AttributeTypeImpl.DateTime.of(this.vertex); }

        @Override
        public boolean isRoot() { return true; }

        @Override
        public void label(java.lang.String label) { throw new HypergraphException(ROOT_TYPE_MUTATION); }

        @Override
        public void isAbstract(boolean isAbstract) { throw new HypergraphException(ROOT_TYPE_MUTATION); }

        @Override
        public void sup(AttributeType superType) { throw new HypergraphException(ROOT_TYPE_MUTATION); }

        @Nullable
        @Override
        public AttributeTypeImpl sup() {
            return null;
        }

        @Override
        public Stream<? extends AttributeTypeImpl> sups() {
            return Stream.of(this);
        }

        @Override
        public Stream<? extends AttributeTypeImpl> subs() {
            return subs(v -> {
                switch (v.valueType()) {
                    case OBJECT:
                        assert this.vertex == v;
                        return this;
                    case BOOLEAN:
                        return AttributeTypeImpl.Boolean.of(v);
                    case LONG:
                        return AttributeTypeImpl.Long.of(v);
                    case DOUBLE:
                        return AttributeTypeImpl.Double.of(v);
                    case STRING:
                        return AttributeTypeImpl.String.of(v);
                    case DATETIME:
                        return AttributeTypeImpl.DateTime.of(v);
                    default:
                        throw new HypergraphException(UNRECOGNISED_VALUE);
                }
            });
        }

        @Override
        public Stream<? extends AttributeImpl<?>> instances() {
            return super.instances(v -> AttributeImpl.of(v.asAttribute()));
        }

        @Override
        public void has(AttributeType attributeType, boolean isKey) {
            throw new HypergraphException(ROOT_TYPE_MUTATION);
        }

        @Override
        public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
            throw new HypergraphException(ROOT_TYPE_MUTATION);
        }

        @Override
        public void plays(RoleType roleType) {
            throw new HypergraphException(ROOT_TYPE_MUTATION);
        }

        @Override
        public void plays(RoleType roleType, RoleType overriddenType) {
            throw new HypergraphException(ROOT_TYPE_MUTATION);
        }

        @Override
        public void unplay(RoleType roleType) {
            throw new HypergraphException(ROOT_TYPE_MUTATION);
        }
    }

    public static class Boolean extends AttributeTypeImpl implements AttributeType.Boolean {

        public Boolean(TypeGraph graph, java.lang.String label) {
            super(graph, label, java.lang.Boolean.class);
        }

        private Boolean(TypeVertex vertex) {
            super(vertex);
            if (!vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) &&
                    !vertex.valueType().equals(Schema.ValueType.BOOLEAN)) {
                throw new HypergraphException(Error.TypeRead.VALUE_TYPE_MISMATCH.format(
                        vertex.label(),
                        Schema.ValueType.BOOLEAN.name(),
                        vertex.valueType().name()
                ));
            }
        }

        public static AttributeTypeImpl.Boolean of(TypeVertex vertex) {
            return vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) ?
                    new Root(vertex) :
                    new AttributeTypeImpl.Boolean(vertex);
        }

        @Nullable
        @Override
        public AttributeTypeImpl.Boolean sup() {
            return super.sup(AttributeTypeImpl.Boolean::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Boolean> sups() {
            return super.sups(AttributeTypeImpl.Boolean::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Boolean> subs() {
            return super.subs(AttributeTypeImpl.Boolean::of);
        }

        @Override
        public Stream<AttributeImpl.Boolean> instances() {
            return super.instances(v -> new AttributeImpl.Boolean(v.asAttribute().asBoolean()));
        }

        @Override
        public Class<java.lang.Boolean> valueType() { return java.lang.Boolean.class; }

        @Override
        public AttributeTypeImpl.Boolean asBoolean() { return this; }

        @Override
        public Attribute.Boolean put(boolean value) {
            return put(value, false);
        }

        @Override
        public Attribute.Boolean put(boolean value, boolean isInferred) {
            validateIsCommitedAndNotAbstract(Attribute.class);
            AttributeVertex<java.lang.Boolean> attVertex = vertex.graph().thing().put(vertex, value, isInferred);
            return new AttributeImpl.Boolean(attVertex);
        }

        @Override
        public Attribute.Boolean get(boolean value) {
            AttributeVertex<java.lang.Boolean> attVertex = vertex.graph().thing().get(vertex, value);
            if (attVertex != null) return new AttributeImpl.Boolean(attVertex);
            else return null;
        }

        private static class Root extends AttributeTypeImpl.Boolean {

            private Root(TypeVertex vertex) {
                super(vertex);
                assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
            }

            @Override
            public boolean isRoot() { return true; }

            @Override
            public Stream<AttributeTypeImpl.Boolean> subs() {
                return stream(apply(
                        super.subTypeVertices(Schema.ValueType.BOOLEAN),
                        AttributeTypeImpl.Boolean::of
                ));
            }

            @Override
            public void label(java.lang.String label) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void isAbstract(boolean isAbstract) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void sup(AttributeType superType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType, RoleType overriddenType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void unplay(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }
        }
    }

    public static class Long extends AttributeTypeImpl implements AttributeType.Long {

        public Long(TypeGraph graph, java.lang.String label) {
            super(graph, label, java.lang.Long.class);
        }

        private Long(TypeVertex vertex) {
            super(vertex);
            if (!vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) &&
                    !vertex.valueType().equals(Schema.ValueType.LONG)) {
                throw new HypergraphException(Error.TypeRead.VALUE_TYPE_MISMATCH.format(
                        vertex.label(),
                        Schema.ValueType.LONG.name(),
                        vertex.valueType().name()
                ));
            }
        }

        public static AttributeTypeImpl.Long of(TypeVertex vertex) {
            return vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) ?
                    new Root(vertex) :
                    new AttributeTypeImpl.Long(vertex);
        }

        @Nullable
        @Override
        public AttributeTypeImpl.Long sup() {
            return super.sup(AttributeTypeImpl.Long::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Long> sups() {
            return super.sups(AttributeTypeImpl.Long::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Long> subs() {
            return super.subs(AttributeTypeImpl.Long::of);
        }

        @Override
        public Stream<AttributeImpl.Long> instances() {
            return super.instances(v -> new AttributeImpl.Long(v.asAttribute().asLong()));
        }

        @Override
        public Class<java.lang.Long> valueType() {
            return java.lang.Long.class;
        }

        @Override
        public AttributeTypeImpl.Long asLong() { return this; }

        @Override
        public Attribute.Long put(long value) {
            return put(value, false);
        }

        @Override
        public Attribute.Long put(long value, boolean isInferred) {
            validateIsCommitedAndNotAbstract(Attribute.class);
            AttributeVertex<java.lang.Long> attVertex = vertex.graph().thing().put(vertex, value, isInferred);
            return new AttributeImpl.Long(attVertex);
        }

        @Override
        public Attribute.Long get(long value) {
            AttributeVertex<java.lang.Long> attVertex = vertex.graph().thing().get(vertex, value);
            if (attVertex != null) return new AttributeImpl.Long(attVertex);
            else return null;
        }

        private static class Root extends AttributeTypeImpl.Long {

            private Root(TypeVertex vertex) {
                super(vertex);
                assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
            }

            @Override
            public boolean isRoot() { return true; }

            @Override
            public Stream<AttributeTypeImpl.Long> subs() {
                return stream(apply(
                        super.subTypeVertices(Schema.ValueType.LONG),
                        AttributeTypeImpl.Long::of
                ));
            }

            @Override
            public void label(java.lang.String label) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void isAbstract(boolean isAbstract) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void sup(AttributeType superType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType, RoleType overriddenType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void unplay(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }
        }
    }

    public static class Double extends AttributeTypeImpl implements AttributeType.Double {

        public Double(TypeGraph graph, java.lang.String label) {
            super(graph, label, java.lang.Double.class);
        }

        private Double(TypeVertex vertex) {
            super(vertex);
            if (!vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) &&
                    !vertex.valueType().equals(Schema.ValueType.DOUBLE)) {
                throw new HypergraphException(Error.TypeRead.VALUE_TYPE_MISMATCH.format(
                        vertex.label(),
                        Schema.ValueType.DOUBLE.name(),
                        vertex.valueType().name()
                ));
            }
        }

        public static AttributeTypeImpl.Double of(TypeVertex vertex) {
            return vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) ?
                    new Root(vertex) :
                    new AttributeTypeImpl.Double(vertex);
        }

        @Nullable
        @Override
        public AttributeTypeImpl.Double sup() {
            return super.sup(AttributeTypeImpl.Double::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Double> sups() {
            return super.sups(AttributeTypeImpl.Double::of);
        }

        @Override
        public Stream<AttributeTypeImpl.Double> subs() {
            return super.subs(AttributeTypeImpl.Double::of);
        }

        @Override
        public Stream<AttributeImpl.Double> instances() {
            return super.instances(v -> new AttributeImpl.Double(v.asAttribute().asDouble()));
        }

        @Override
        public Class<java.lang.Double> valueType() {
            return java.lang.Double.class;
        }

        @Override
        public AttributeTypeImpl.Double asDouble() { return this; }

        @Override
        public Attribute.Double put(double value) {
            return put(value, false);
        }

        @Override
        public Attribute.Double put(double value, boolean isInferred) {
            validateIsCommitedAndNotAbstract(Attribute.class);
            AttributeVertex<java.lang.Double> attVertex = vertex.graph().thing().put(vertex, value, isInferred);
            return new AttributeImpl.Double(attVertex);
        }

        @Override
        public Attribute.Double get(double value) {
            AttributeVertex<java.lang.Double> attVertex = vertex.graph().thing().get(vertex, value);
            if (attVertex != null) return new AttributeImpl.Double(attVertex);
            else return null;
        }

        private static class Root extends AttributeTypeImpl.Double {

            private Root(TypeVertex vertex) {
                super(vertex);
                assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
            }

            @Override
            public boolean isRoot() { return true; }

            @Override
            public Stream<AttributeTypeImpl.Double> subs() {
                return stream(apply(
                        super.subTypeVertices(Schema.ValueType.DOUBLE),
                        AttributeTypeImpl.Double::of
                ));
            }

            @Override
            public void label(java.lang.String label) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void isAbstract(boolean isAbstract) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void sup(AttributeType superType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType, RoleType overriddenType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void unplay(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }
        }
    }

    public static class String extends AttributeTypeImpl implements AttributeType.String {

        public String(TypeGraph graph, java.lang.String label) {
            super(graph, label, java.lang.String.class);
        }

        private String(TypeVertex vertex) {
            super(vertex);
            if (!vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) &&
                    !vertex.valueType().equals(Schema.ValueType.STRING)) {
                throw new HypergraphException(Error.TypeRead.VALUE_TYPE_MISMATCH.format(
                        vertex.label(),
                        Schema.ValueType.STRING.name(),
                        vertex.valueType().name()
                ));
            }
        }

        public static AttributeTypeImpl.String of(TypeVertex vertex) {
            return vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) ?
                    new Root(vertex) :
                    new AttributeTypeImpl.String(vertex);
        }

        @Nullable
        @Override
        public AttributeTypeImpl.String sup() {
            return super.sup(AttributeTypeImpl.String::of);
        }

        @Override
        public Stream<AttributeTypeImpl.String> sups() {
            return super.sups(AttributeTypeImpl.String::of);
        }

        @Override
        public Stream<AttributeTypeImpl.String> subs() {
            return super.subs(AttributeTypeImpl.String::of);
        }

        @Override
        public Stream<AttributeImpl.String> instances() {
            return super.instances(v -> new AttributeImpl.String(v.asAttribute().asString()));
        }

        @Override
        public AttributeTypeImpl.String asString() { return this; }

        @Override
        public Attribute.String put(java.lang.String value) {
            return put(value, false);
        }

        @Override
        public Attribute.String put(java.lang.String value, boolean isInferred) {
            validateIsCommitedAndNotAbstract(Attribute.class);
            if (value.length() > Schema.STRING_MAX_LENGTH) {
                throw new HypergraphException(Error.ThingWrite.ILLEGAL_STRING_SIZE);
            }
            AttributeVertex<java.lang.String> attVertex = vertex.graph().thing().put(vertex, value, isInferred);
            return new AttributeImpl.String(attVertex);
        }

        @Override
        public Attribute.String get(java.lang.String value) {
            AttributeVertex<java.lang.String> attVertex = vertex.graph().thing().get(vertex, value);
            if (attVertex != null) return new AttributeImpl.String(attVertex);
            else return null;
        }

        @Override
        public Class<java.lang.String> valueType() {
            return java.lang.String.class;
        }

        private static class Root extends AttributeTypeImpl.String {

            private Root(TypeVertex vertex) {
                super(vertex);
                assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
            }

            @Override
            public boolean isRoot() { return true; }

            @Override
            public Stream<AttributeTypeImpl.String> subs() {
                return stream(apply(
                        super.subTypeVertices(Schema.ValueType.STRING),
                        AttributeTypeImpl.String::of
                ));
            }

            @Override
            public void label(java.lang.String label) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void isAbstract(boolean isAbstract) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void sup(AttributeType superType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType, RoleType overriddenType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void unplay(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }
        }
    }

    public static class DateTime extends AttributeTypeImpl implements AttributeType.DateTime {

        public DateTime(TypeGraph graph, java.lang.String label) {
            super(graph, label, LocalDateTime.class);
        }

        private DateTime(TypeVertex vertex) {
            super(vertex);
            if (!vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) &&
                    !vertex.valueType().equals(Schema.ValueType.DATETIME)) {
                throw new HypergraphException(Error.TypeRead.VALUE_TYPE_MISMATCH.format(
                        vertex.label(),
                        Schema.ValueType.DATETIME.name(),
                        vertex.valueType().name()
                ));
            }
        }

        public static AttributeTypeImpl.DateTime of(TypeVertex vertex) {
            return vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label()) ?
                    new Root(vertex) :
                    new AttributeTypeImpl.DateTime(vertex);
        }

        @Nullable
        @Override
        public AttributeTypeImpl.DateTime sup() {
            return super.sup(AttributeTypeImpl.DateTime::of);
        }

        @Override
        public Stream<AttributeTypeImpl.DateTime> sups() {
            return super.sups(AttributeTypeImpl.DateTime::of);
        }

        @Override
        public Stream<AttributeTypeImpl.DateTime> subs() {
            return super.subs(AttributeTypeImpl.DateTime::of);
        }

        @Override
        public Stream<AttributeImpl.DateTime> instances() {
            return super.instances(v -> new AttributeImpl.DateTime(v.asAttribute().asDateTime()));
        }

        @Override
        public Class<LocalDateTime> valueType() {
            return LocalDateTime.class;
        }

        @Override
        public AttributeTypeImpl.DateTime asDateTime() { return this; }

        @Override
        public Attribute.DateTime put(LocalDateTime value) {
            return put(value, false);
        }

        @Override
        public Attribute.DateTime put(LocalDateTime value, boolean isInferred) {
            validateIsCommitedAndNotAbstract(Attribute.class);
            AttributeVertex<LocalDateTime> attVertex = vertex.graph().thing().put(vertex, value, isInferred);
            if (!isInferred && attVertex.isInferred()) attVertex.isInferred(false);
            return new AttributeImpl.DateTime(attVertex);
        }

        @Override
        public Attribute.DateTime get(LocalDateTime value) {
            AttributeVertex<java.time.LocalDateTime> attVertex = vertex.graph().thing().get(vertex, value);
            if (attVertex != null) return new AttributeImpl.DateTime(attVertex);
            else return null;
        }

        private static class Root extends AttributeTypeImpl.DateTime {

            private Root(TypeVertex vertex) {
                super(vertex);
                assert vertex.label().equals(Schema.Vertex.Type.Root.ATTRIBUTE.label());
            }

            @Override
            public boolean isRoot() { return true; }

            @Override
            public Stream<AttributeTypeImpl.DateTime> subs() {
                return stream(apply(
                        super.subTypeVertices(Schema.ValueType.DATETIME),
                        AttributeTypeImpl.DateTime::of
                ));
            }

            @Override
            public void label(java.lang.String label) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void isAbstract(boolean isAbstract) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void sup(AttributeType superType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void has(AttributeType attributeType, AttributeType overriddenType, boolean isKey) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void plays(RoleType roleType, RoleType overriddenType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }

            @Override
            public void unplay(RoleType roleType) {
                throw new HypergraphException(ROOT_TYPE_MUTATION);
            }
        }
    }
}
