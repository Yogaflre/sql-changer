package com.yogafire.sql.changer.tree;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Created by yogafire on 2020/7/23
 */
public class DistributeBy extends Node {
    private final List<Expression> expression;

    public DistributeBy(List<Expression> expression) {
        this(Optional.empty(), expression);
    }

    public DistributeBy(NodeLocation location, List<Expression> expression) {
        this(Optional.of(location), expression);
    }

    public DistributeBy(Optional<NodeLocation> location, List<Expression> expression) {
        super(location);
        this.expression = expression;
    }

    public List<Expression> getExpression() {
        return expression;
    }

    @Override
    protected <R, C> R accept(AstVisitor<R, C> visitor, C context) {
        return visitor.visitDistributeBy(this, context);
    }

    @Override
    public List<? extends Node> getChildren() {
        return expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        DistributeBy c = (DistributeBy) obj;
        return Objects.equals(expression, c.expression);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("expression", expression)
                .toString();
    }
}
