/*
 * Copyright (c) 2015 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.timetree.module;

import com.graphaware.module.timetree.CustomRootTimeTree;
import com.graphaware.module.timetree.SingleTimeTree;
import com.graphaware.module.timetree.TimeTreeBackedEvents;
import com.graphaware.module.timetree.TimedEvents;
import com.graphaware.module.timetree.domain.TimeInstant;
import com.graphaware.runtime.config.TxDrivenModuleConfiguration;
import com.graphaware.runtime.module.BaseTxDrivenModule;
import com.graphaware.runtime.module.DeliberateTransactionRollbackException;
import com.graphaware.tx.event.improved.api.Change;
import com.graphaware.tx.event.improved.api.ImprovedTransactionData;
import com.graphaware.tx.executor.batch.BatchTransactionExecutor;
import com.graphaware.tx.executor.batch.IterableInputBatchTransactionExecutor;
import com.graphaware.tx.executor.batch.UnitOfWork;
import com.graphaware.tx.executor.input.TransactionalInput;
import com.graphaware.tx.executor.single.TransactionCallback;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.graphaware.common.util.PropertyContainerUtils.getLong;

/**
 * A {@link com.graphaware.runtime.module.TxDrivenModule} that automatically attaches events to a {@link com.graphaware.module.timetree.TimeTree}.
 */
public class TimeTreeModule extends BaseTxDrivenModule<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTreeModule.class);

    private final TimeTreeConfiguration configuration;
    private final TimedEvents timedEvents;

    public TimeTreeModule(String moduleId, TimeTreeConfiguration configuration, GraphDatabaseService database) {
        super(moduleId);
        this.configuration = configuration;
        this.timedEvents = new TimeTreeBackedEvents(new SingleTimeTree(database));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TxDrivenModuleConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void beforeCommit(ImprovedTransactionData transactionData) throws DeliberateTransactionRollbackException {
        for (Node created : transactionData.getAllCreatedNodes()) {
            createTimeTreeRelationship(created);
        }

        for (Change<Node> change : transactionData.getAllChangedNodes()) {
            if (transactionData.hasPropertyBeenChanged(change.getPrevious(), configuration.getTimestampProperty())
                    || transactionData.hasPropertyBeenChanged(change.getPrevious(), configuration.getCustomTimeTreeRootProperty())
                    || transactionData.hasPropertyBeenDeleted(change.getPrevious(), configuration.getTimestampProperty())
                    || transactionData.hasPropertyBeenDeleted(change.getPrevious(), configuration.getCustomTimeTreeRootProperty())) {

                deleteTimeTreeRelationship(change.getPrevious());
                createTimeTreeRelationship(change.getCurrent());
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final GraphDatabaseService database) {
        if (!configuration.isAutoAttach() || !configuration.getInitializeLabelsRestriction().hasLabelsRestriction()) {
            return;
        }

        BatchTransactionExecutor executor = new IterableInputBatchTransactionExecutor<>(database, 1000,
                new TransactionalInput<>(database, 1000, new TransactionCallback<Iterable<Node>>() {
                    @Override
                    public Iterable<Node> doInTransaction(GraphDatabaseService graphDatabaseService) throws Exception {

                        return database.getAllNodes();
                    }
                }),
                new UnitOfWork<Node>() {
                    @Override
                    public void execute(GraphDatabaseService database, Node input, int batchNumber, int stepNumber) {
                        if (stepNumber == 1) {
                            LOG.info("Attaching existing events to TimeTree in batch " + batchNumber);
                        }
                        if (configuration.getInclusionPolicies().getNodeInclusionPolicy().include(input)) {
                            deleteTimeTreeRelationship(input);
                            createTimeTreeRelationship(input);
                        }
                    }
                }
        );

        executor.execute();


    }

    private void createTimeTreeRelationship(Node created) {
        if (!created.hasProperty(configuration.getTimestampProperty())) {
            LOG.warn("Created node with ID " + created.getId() + " does not have a " + configuration.getTimestampProperty() + " property!");
            return;
        }

        Long timestamp;
        try {
            timestamp = (Long) created.getProperty(configuration.getTimestampProperty());
        } catch (Throwable throwable) {
            LOG.warn("Created node with ID " + created.getId() + " does not have a valid timestamp property", throwable);
            return;
        }

        TimedEvents timedEventsToUse;
        if (configuration.getCustomTimeTreeRootProperty() != null && created.hasProperty(configuration.getCustomTimeTreeRootProperty())) {
            timedEventsToUse = new TimeTreeBackedEvents(new CustomRootTimeTree(created.getGraphDatabase().getNodeById(getLong(created, configuration.getCustomTimeTreeRootProperty()))));
        } else if (configuration.getDynamicRoot().isDefined() && created.hasProperty(configuration.getDynamicRoot().getRootPropertyValueRef())){
            Label rootLabel = DynamicLabel.label(configuration.getDynamicRoot().getRootLabel());
            Node root = created.getGraphDatabase().findNode(rootLabel, configuration.getDynamicRoot().getRootPropertyNameRef(), created.getProperty(configuration.getDynamicRoot().getRootPropertyValueRef()));
            if (root != null) {
                timedEventsToUse = new TimeTreeBackedEvents(new CustomRootTimeTree(root));
            } else {
                timedEventsToUse = timedEvents;
            }
        } else {
            timedEventsToUse = timedEvents;
        }

        timedEventsToUse.attachEvent(created, configuration.getRelationshipType(), TimeInstant.instant(timestamp).with(configuration.getResolution()).with(configuration.getTimeZone()));
    }

    private void deleteTimeTreeRelationship(Node changed) {
        for (Relationship r : changed.getRelationships(Direction.OUTGOING, configuration.getRelationshipType())) {
            r.delete();
        }
    }
}
