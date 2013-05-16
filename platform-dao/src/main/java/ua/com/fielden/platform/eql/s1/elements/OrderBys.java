package ua.com.fielden.platform.eql.s1.elements;

import java.util.ArrayList;
import java.util.List;

public class OrderBys implements IElement {
    private final List<OrderBy> models;

    public OrderBys(final List<OrderBy> models) {
	this.models = models;
    }

    @Override
    public String toString() {
	return models.toString();
    }

    @Override
    public List<EntValue> getAllValues() {
	final List<EntValue> result = new ArrayList<EntValue>();
	for (final OrderBy model : models) {
	    if (model.getOperand() != null) {
		result.addAll(model.getOperand().getAllValues());
	    }
	}
	return result;
    }

    @Override
    public List<EntQuery> getLocalSubQueries() {
	final List<EntQuery> result = new ArrayList<EntQuery>();
	for (final OrderBy model : models) {
	    if (model.getOperand() != null) {
		result.addAll(model.getOperand().getLocalSubQueries());
	    }
	}
	return result;
    }

    @Override
    public List<EntProp> getLocalProps() {
	final List<EntProp> result = new ArrayList<EntProp>();
	for (final OrderBy model : models) {
	    if (model.getOperand() != null) {
		result.addAll(model.getOperand().getLocalProps());
	    }
	}
	return result;
    }

    public List<OrderBy> getModels() {
	return models;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((models == null) ? 0 : models.hashCode());
	return result;
    }

    @Override
    public boolean equals(final Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (!(obj instanceof OrderBys)) {
	    return false;
	}
	final OrderBys other = (OrderBys) obj;
	if (models == null) {
	    if (other.models != null) {
		return false;
	    }
	} else if (!models.equals(other.models)) {
	    return false;
	}
	return true;
    }

    @Override
    public boolean ignore() {
	// TODO Auto-generated method stub
	return false;
    }
}