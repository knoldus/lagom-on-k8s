package com.knoldus.lagom.sample.restaurant.menu;

import akka.Done;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.google.inject.Inject;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import org.pcollections.PSequence;

import java.util.List;
import java.util.concurrent.CompletionStage;

public final class MenuEventProcessor extends ReadSideProcessor<MenuEvent> {

    private final CassandraSession cassandraSession;
    private final CassandraReadSide cassandraReadSide;

    private PreparedStatement insertItem;

    @Inject
    private MenuEventProcessor(final CassandraSession cassandraSession, final CassandraReadSide cassandraReadSide) {
        this.cassandraSession = cassandraSession;
        this.cassandraReadSide = cassandraReadSide;
    }

    @Override
    public ReadSideHandler<MenuEvent> buildHandler() {
        return cassandraReadSide.<MenuEvent>builder("menu_offset")
                .setGlobalPrepare(this::createTable)
                .setPrepare(tag -> prepareInsertItem())
                .setEventHandler(MenuEvent.ItemAdded.class, event -> insertItem(event.getItem()))
                .build();
    }

    @Override
    public PSequence<AggregateEventTag<MenuEvent>> aggregateTags() {
        return null;
    }

    private CompletionStage<Done> createTable() {
        return cassandraSession.executeCreateTable(
                "CREATE TABLE IF NOT EXISTS menu ("
                        + "name text"
                        + "PRIMARY KEY name)");
    }

    private CompletionStage<Done> prepareInsertItem() {
        return cassandraSession.prepare("INSERT INTO menu (name) VALUES (?)")
                .thenApply(s -> {
                    insertItem = s;
                    return Done.getInstance();
                });
    }

    private CompletionStage<List<BoundStatement>> insertItem(Item item) {
        return CassandraReadSide.completedStatement(insertItem.bind(item.getName()));
    }

}
