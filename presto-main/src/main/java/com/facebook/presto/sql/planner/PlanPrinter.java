package com.facebook.presto.sql.planner;

import com.facebook.presto.sql.ExpressionFormatter;
import com.facebook.presto.sql.compiler.Slot;
import com.facebook.presto.sql.tree.Expression;
import com.facebook.presto.sql.tree.FunctionCall;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Map;

public class PlanPrinter
{
    public void print(PlanNode plan)
    {
        Visitor visitor = new Visitor();
        plan.accept(visitor, 0);
    }

    private void print(int indent, String format, Object... args)
    {
        String value;

        if (args.length == 0) {
            value = format;
        }
        else {
            value = String.format(format, args);
        }

        System.out.println(Strings.repeat("    ", indent) + value);
    }

    private String formatOutputs(List<Slot> slots)
    {
        return Joiner.on(", ").join(Iterables.transform(slots, new Function<Slot, String>()
        {
            @Override
            public String apply(Slot input)
            {
                return input.getName() + ":" + input.getType();
            }
        }));
    }

    private class Visitor
            extends PlanVisitor<Integer, Void>
    {
        @Override
        public Void visitLimit(LimitNode node, Integer indent)
        {
            print(indent, "- Limit => [%s]", formatOutputs(node.getOutputs()));
            print(indent + 2, "count = %s", node.getCount());

            return processChildren(node, indent + 1);
        }

        @Override
        public Void visitAggregation(AggregationNode node, Integer indent)
        {
            print(indent, "- Aggregate => [%s]", formatOutputs(node.getOutputs()));

            if (!node.getGroupBy().isEmpty()) {
                print(indent + 2, "key = %s", Joiner.on(", ").join(node.getGroupBy()));
            }

            for (Map.Entry<Slot, FunctionCall> entry : node.getAggregations().entrySet()) {
                print(indent + 2, "%s := %s", entry.getKey(), ExpressionFormatter.toString(entry.getValue()));

            }

            return processChildren(node, indent + 1);
        }

        @Override
        public Void visitTableScan(TableScan node, Integer indent)
        {
            print(indent, "- TableScan[%s.%s.%s] => [%s]", node.getCatalogName(), node.getSchemaName(), node.getTableName(), formatOutputs(node.getOutputs()));
            for (Map.Entry<String, Slot> entry : node.getAttributes().entrySet()) {
                print(indent + 2, "%s := %s", entry.getKey(), entry.getValue());
            }

            return null;
        }

        @Override
        public Void visitFilter(FilterNode node, Integer indent)
        {
            print(indent, "- Filter => [%s]", formatOutputs(node.getOutputs()));
            print(indent + 2, "predicate = %s", ExpressionFormatter.toString(node.getPredicate()));

            return processChildren(node, indent + 1);
        }

        @Override
        public Void visitProject(ProjectNode node, Integer indent)
        {
            print(indent, "- Project => [%s]", formatOutputs(node.getOutputs()));
            for (Map.Entry<Slot, Expression> entry : node.getOutputMap().entrySet()) {
                print(indent + 2, "%s := %s", entry.getKey(), ExpressionFormatter.toString(entry.getValue()));
            }

            return processChildren(node, indent + 1);
        }

        @Override
        public Void visitOutput(OutputPlan node, Integer indent)
        {
            print(indent, "- Output[%s]", Joiner.on(", ").join(node.getColumnNames()));
            for (int i = 0; i < node.getColumnNames().size(); i++) {
                String name = node.getColumnNames().get(i);
                print(indent + 2, "%s := %s", name, node.getAssignments().get(name));
            }

            return processChildren(node, indent + 1);
        }

        @Override
        public Void visitTopN(TopNNode node, Integer indent)
        {
            print(indent, "- TopN => [%s]", formatOutputs(node.getOutputs()));
            print(indent + 2, "key = %s", node.getOrderBy());
            print(indent + 2, "order = %s", node.getOrderings());
            print(indent + 2, "count = %s", node.getCount());

            return processChildren(node, indent + 1);
        }

        @Override
        protected Void visitPlan(PlanNode node, Integer context)
        {
            throw new UnsupportedOperationException("not yet implemented");
        }

        private Void processChildren(PlanNode node, int indent)
        {
            for (PlanNode child : node.getSources()) {
                child.accept(this, indent);
            }

            return null;
        }
    }
}