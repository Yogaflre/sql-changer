package com.yogafire.sql.changer.tree;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Created by yogafire on 2020/7/23
 */
public class SortBy extends Node {

    private final List<SortItem> sortItems;

    public SortBy(List<SortItem> sortItems) {
        this(Optional.empty(), sortItems);
    }

    public SortBy(NodeLocation location, List<SortItem> sortItems) {
        this(Optional.of(location), sortItems);
    }

    private SortBy(Optional<NodeLocation> location, List<SortItem> sortItems) {
        super(location);
        requireNonNull(sortItems, "sortItems is null");
        checkArgument(!sortItems.isEmpty(), "sortItems should not be empty");
        this.sortItems = ImmutableList.copyOf(sortItems);
    }

    public List<SortItem> getSortItems() {
        return sortItems;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitSortBy(this, context);
    }

    @Override
    public List<? extends Node> getChildren() {
        return sortItems;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("sortItems", sortItems)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        SortBy o = (SortBy) obj;
        return Objects.equals(sortItems, o.sortItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortItems);
    }
}
