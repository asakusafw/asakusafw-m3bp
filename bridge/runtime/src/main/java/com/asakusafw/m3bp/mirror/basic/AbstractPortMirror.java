/**
 * Copyright 2011-2016 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.m3bp.mirror.basic;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.dag.api.common.KeyValueSerDe;
import com.asakusafw.dag.api.common.ValueSerDe;
import com.asakusafw.dag.utils.common.Arguments;
import com.asakusafw.dag.utils.common.Invariants;
import com.asakusafw.m3bp.descriptor.M3bpEdgeDescriptor;
import com.asakusafw.m3bp.mirror.Movement;
import com.asakusafw.m3bp.mirror.PortMirror;

/**
 * A mirror of M3BP {@code InputPort} and {@code OutputPort}.
 */
public abstract class AbstractPortMirror implements PortMirror {

    static final Logger LOG = LoggerFactory.getLogger(AbstractPortMirror.class);

    private final Set<PortMirror> opposites = new LinkedHashSet<>();

    /**
     * Returns the descriptor.
     * @return the descriptor
     */
    protected abstract M3bpEdgeDescriptor getDescriptor();

    @Override
    public ValueSerDe newValueSerDe(ClassLoader loader) {
        Arguments.requireNonNull(loader);
        Invariants.require(hasKey() == false);
        Invariants.require(hasValue());
        return (ValueSerDe) getDescriptor().getSerDe().newInstance(loader).get();
    }

    @Override
    public KeyValueSerDe newKeyValueSerDe(ClassLoader loader) {
        Arguments.requireNonNull(loader);
        Invariants.require(hasKey());
        Invariants.require(hasValue());
        return (KeyValueSerDe) getDescriptor().getSerDe().newInstance(loader).get();
    }

    @Override
    public String getValueComparatorName() {
        return getDescriptor().getValueComparatorName();
    }

    @Override
    public Movement getMovement() {
        return getDescriptor().getMovement();
    }

    @Override
    public boolean hasKey() {
        return getMovement() == Movement.SCATTER_GATHER;
    }

    @Override
    public boolean hasValue() {
        return getMovement() != Movement.NOTHING;
    }

    static void connect(AbstractPortMirror upstream, AbstractPortMirror downstream) {
        Arguments.requireNonNull(upstream);
        Arguments.requireNonNull(downstream);
        upstream.addOpposite(downstream);
        downstream.addOpposite(upstream);
    }

    @Override
    public Set<? extends PortMirror> getOpposites() {
        return Collections.unmodifiableSet(opposites);
    }

    private void addOpposite(PortMirror opposite) {
        if (opposites.contains(opposite)) {
            LOG.warn(MessageFormat.format(
                    "already connected: {0}@{1} <=> {2}@{3}", //$NON-NLS-1$
                    getOwner().getName(),
                    getName(),
                    opposite.getOwner().getName(),
                    opposite.getName()));
        } else {
            opposites.add(opposite);
        }
    }
}
