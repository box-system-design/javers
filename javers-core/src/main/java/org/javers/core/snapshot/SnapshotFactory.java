package org.javers.core.snapshot;

import org.javers.common.collections.Defaults;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.graph.Cdo;
import org.javers.core.graph.LiveNode;
import org.javers.core.metamodel.object.*;
import org.javers.core.metamodel.type.JaversProperty;
import org.javers.core.metamodel.type.ManagedType;
import org.javers.core.metamodel.type.TypeMapper;

import java.util.Objects;

import static org.javers.core.metamodel.object.CdoSnapshotBuilder.cdoSnapshot;
import static org.javers.core.metamodel.object.SnapshotType.*;

/**
 * @author bartosz walacik
 */
public class SnapshotFactory {
    private final TypeMapper typeMapper;

    SnapshotFactory(TypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    public CdoSnapshot createTerminal(GlobalId globalId, CdoSnapshot previous, CommitMetadata commitMetadata) {
        ManagedType managedType = typeMapper.getJaversManagedType(globalId);
        return cdoSnapshot()
                .withGlobalId(globalId)
                .withManagedType(managedType)
                .withCommitMetadata(commitMetadata)
                .withType(TERMINAL)
                .withVersion(previous.getVersion()+1)
                .build();
    }

    CdoSnapshot createInitial(LiveNode liveNode, CommitMetadata commitMetadata) {
        return initSnapshotBuilder(liveNode, commitMetadata)
                .withState(createSnapshotState(liveNode))
                .withType(INITIAL)
                .markAllAsChanged()
                .withVersion(1L)
                .build();
    }

    CdoSnapshot createUpdate(LiveNode liveNode, CdoSnapshot previous, CommitMetadata commitMetadata) {
        return initSnapshotBuilder(liveNode, commitMetadata)
                .withState(createSnapshotState(liveNode))
                .withType(UPDATE)
                .markChanged(previous)
                .withVersion(previous.getVersion()+1)
                .build();
    }

    public CdoSnapshotState createSnapshotStateNoRefs(Cdo liveCdo){
        CdoSnapshotStateBuilder stateBuilder = CdoSnapshotStateBuilder.cdoSnapshotState();
        for (JaversProperty property : liveCdo.getManagedType().getProperties()) {
            if (typeMapper.isManagedType(property.getType()) ||
                typeMapper.isEnumerableOfManagedTypes(property.getType())) {
                continue;
            }

            Object propertyValue = liveCdo.getPropertyValue(property);
            if (Objects.equals(propertyValue, Defaults.defaultValue(property.getGenericType()))) {
                continue;
            }
            stateBuilder.withPropertyValue(property, propertyValue);
        }
        return stateBuilder.build();
    }

    public CdoSnapshotState createSnapshotState(LiveNode liveNode){
        CdoSnapshotStateBuilder stateBuilder = CdoSnapshotStateBuilder.cdoSnapshotState();
        for (JaversProperty property : liveNode.getManagedType().getProperties()) {
            Object dehydratedPropertyValue = liveNode.getDehydratedPropertyValue(property);
            if (Objects.equals(dehydratedPropertyValue, Defaults.defaultValue(property.getGenericType()))) {
                continue;
            }
            stateBuilder.withPropertyValue(property, dehydratedPropertyValue);
        }
        return stateBuilder.build();
    }

    private CdoSnapshotBuilder initSnapshotBuilder(LiveNode liveNode, CommitMetadata commitMetadata) {
        return cdoSnapshot()
                .withGlobalId(liveNode.getGlobalId())
                .withCommitMetadata(commitMetadata)
                .withManagedType(liveNode.getManagedType());
    }
}
