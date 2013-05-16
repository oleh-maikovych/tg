package ua.com.fielden.platform.eql.s1.processing;

import java.util.Map;

import ua.com.fielden.platform.eql.s1.elements.LowerCaseOf;

public class LowerCaseOfBuilder extends OneArgumentFunctionBuilder {

    protected LowerCaseOfBuilder(final AbstractTokensBuilder parent, final EntQueryGenerator queryBuilder, final Map<String, Object> paramValues) {
	super(parent, queryBuilder, paramValues);
    }

    @Override
    Object getModel() {
	return new LowerCaseOf(getModelForSingleOperand(firstCat(), firstValue()));
    }
}