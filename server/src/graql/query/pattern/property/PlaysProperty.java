/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
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
 */

package grakn.core.graql.query.pattern.property;

import com.google.common.collect.ImmutableSet;
import grakn.core.graql.admin.Atomic;
import grakn.core.graql.admin.ReasonerQuery;
import grakn.core.graql.concept.ConceptId;
import grakn.core.graql.concept.Role;
import grakn.core.graql.concept.Thing;
import grakn.core.graql.concept.Type;
import grakn.core.graql.exception.GraqlQueryException;
import grakn.core.graql.internal.gremlin.EquivalentFragmentSet;
import grakn.core.graql.internal.gremlin.sets.EquivalentFragmentSets;
import grakn.core.graql.internal.reasoner.atom.binary.PlaysAtom;
import grakn.core.graql.internal.reasoner.atom.predicate.IdPredicate;
import grakn.core.graql.query.pattern.Statement;
import grakn.core.graql.query.pattern.Variable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static grakn.core.graql.internal.reasoner.utils.ReasonerUtils.getIdPredicate;

/**
 * Reperesents the {@code plays} property on a {@link Type}.
 * This property relates a {@link Type} and a {@link Role}. It indicates that an
 * {@link Thing} whose type is this {@link Type} is permitted to be a role-player
 * playing the role of the given {@link Role}.
 */
public class PlaysProperty extends VarProperty {

    public static final String NAME = "plays";
    private final Statement role;
    private final boolean required;

    public PlaysProperty(Statement role, boolean required) {
        if (role == null) {
            throw new NullPointerException("Null role");
        }
        this.role = role;
        this.required = required;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProperty() {
        return role.getPrintableName();
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public Collection<EquivalentFragmentSet> match(Variable start) {
        return ImmutableSet.of(EquivalentFragmentSets.plays(this, start, role.var(), required));
    }

    @Override
    public Stream<Statement> getTypes() {
        return Stream.of(role);
    }

    @Override
    public Stream<Statement> innerStatements() {
        return Stream.of(role);
    }

    @Override
    public Atomic mapToAtom(Statement var, Set<Statement> vars, ReasonerQuery parent) {
        Variable varName = var.var().asUserDefined();
        Statement typeVar = role;
        Variable typeVariable = typeVar.var();
        IdPredicate predicate = getIdPredicate(typeVariable, typeVar, vars, parent);
        ConceptId predicateId = predicate == null ? null : predicate.getPredicate();
        return PlaysAtom.create(varName, typeVariable, predicateId, parent);
    }

    @Override
    public Collection<PropertyExecutor> define(Variable var) throws GraqlQueryException {
        PropertyExecutor.Method method = executor -> {
            Role role = executor.get(this.role.var()).asRole();
            executor.get(var).asType().plays(role);
        };

        return ImmutableSet.of(PropertyExecutor.builder(method).requires(var, role.var()).build());
    }

    @Override
    public Collection<PropertyExecutor> undefine(Variable var) throws GraqlQueryException {
        PropertyExecutor.Method method = executor -> {
            Type type = executor.get(var).asType();
            Role role = executor.get(this.role.var()).asRole();

            if (!type.isDeleted() && !role.isDeleted()) {
                type.unplay(role);
            }
        };

        return ImmutableSet.of(PropertyExecutor.builder(method).requires(var, role.var()).build());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PlaysProperty) {
            PlaysProperty that = (PlaysProperty) o;
            return (this.role.equals(that.role))
                    && (this.required == that.required);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= this.role.hashCode();
        h *= 1000003;
        h ^= this.required ? 1231 : 1237;
        return h;
    }
}