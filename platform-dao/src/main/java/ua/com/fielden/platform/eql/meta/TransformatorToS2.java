package ua.com.fielden.platform.eql.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ua.com.fielden.platform.entity.AbstractEntity;
import ua.com.fielden.platform.entity.query.EntityAggregates;
import ua.com.fielden.platform.eql.s1.elements.EntProp1;
import ua.com.fielden.platform.eql.s1.elements.EntQuery1;
import ua.com.fielden.platform.eql.s1.elements.ISource1;
import ua.com.fielden.platform.eql.s1.elements.QueryBasedSource1;
import ua.com.fielden.platform.eql.s1.elements.TypeBasedSource1;
import ua.com.fielden.platform.eql.s2.elements.EntProp2;
import ua.com.fielden.platform.eql.s2.elements.EntQuery2;
import ua.com.fielden.platform.eql.s2.elements.Expression2;
import ua.com.fielden.platform.eql.s2.elements.ISource2;
import ua.com.fielden.platform.eql.s2.elements.QueryBasedSource2;
import ua.com.fielden.platform.eql.s2.elements.TypeBasedSource2;
import ua.com.fielden.platform.eql.s2.elements.Yield2;

public class TransformatorToS2 {
    private List<Map<ISource1<? extends ISource2>, SourceInfo>> sourceMap = new ArrayList<>();
    private final Map<Class<? extends AbstractEntity<?>>, EntityInfo> metadata;


    static class SourceInfo {
	private final ISource2 source;
	private final EntityInfo entityInfo;
	private final boolean aliasingAllowed;

	public SourceInfo(final ISource2 source, final EntityInfo entityInfo, final boolean aliasingAllowed) {
	    this.source = source;
	    this.entityInfo = entityInfo;
	    this.aliasingAllowed = aliasingAllowed;
	}

	SourceInfo produceNewWithoutAliasing() {
	    return new SourceInfo(source, entityInfo, false);
	}
    }

    public TransformatorToS2(final Map<Class<? extends AbstractEntity<?>>, EntityInfo> metadata) {
	this.metadata = metadata;
	sourceMap.add(new HashMap<ISource1<? extends ISource2>, SourceInfo>());
    }

    @Override
    public String toString() {
	final StringBuffer sb = new StringBuffer();
	for (final Map<ISource1<? extends ISource2>, SourceInfo> item : sourceMap) {
	    sb.append("-----------------------------\n");
	    for (final SourceInfo subitem : item.values()) {
		sb.append("---");
		sb.append(subitem.source.sourceType().getSimpleName());
		sb.append("\n");
	    }
	}
	return sb.toString();
    }

    public void addSource(final ISource1<? extends ISource2> source) {
	System.out.println("              sourceType = " + source.sourceType() + "; " + metadata.get(source.sourceType()));
	final ISource2 transformedSource = transformSource(source);
	if (EntityAggregates.class.equals(transformedSource.sourceType())) {
	    final EntityInfo entAggEntityInfo = new EntityInfo(EntityAggregates.class);
	    for (final Yield2 yield : ((QueryBasedSource2) transformedSource).getYields().getYields()) {
		final AbstractPropInfo aep = yield.javaType().isAssignableFrom(AbstractEntity.class) ? new EntityTypePropInfo(yield.getAlias(), entAggEntityInfo, metadata.get(yield.javaType()), null) :
		    new PrimTypePropInfo(yield.getAlias(), entAggEntityInfo, yield.javaType(), null);
		//System.out.println("putting -- " + yield.getAlias() + " ... " + aep);
		entAggEntityInfo.getProps().put(yield.getAlias(), aep);
	    }

	    getCurrentQueryMap().put(source, new SourceInfo(transformedSource, entAggEntityInfo, true));
	} else {
	    getCurrentQueryMap().put(source, new SourceInfo(transformedSource, metadata.get(transformedSource.sourceType()), true));
	}
    }

    public TransformatorToS2 produceBasedOn() {
	final TransformatorToS2 result = new TransformatorToS2(metadata);
	result.sourceMap.addAll(sourceMap);
	return result;
    }

    public TransformatorToS2 produceNewOne() {
	final TransformatorToS2 result = new TransformatorToS2(metadata);
	return result;
    }

    public TransformatorToS2 produceOneForCalcPropExpression(final ISource2 source) {
	final TransformatorToS2 result = new TransformatorToS2(metadata);
	for (final Map<ISource1<? extends ISource2>, SourceInfo> item : sourceMap) {
	    for (final Entry<ISource1<? extends ISource2>, SourceInfo> mapItem : item.entrySet()) {
		if (mapItem.getValue().source.equals(source)) {
		    final Map<ISource1<? extends ISource2>, SourceInfo> newMap = new HashMap<>();
		    newMap.put(mapItem.getKey(), mapItem.getValue().produceNewWithoutAliasing());
		    result.sourceMap.add(newMap);
		    return result;
		}
	    }

	}

	throw new IllegalStateException("Should not reach here!");
    }

    private ISource2 transformSource(final ISource1<? extends ISource2> originalSource) {
//	if (originalSource.sourceType() == EntityAggregates.class) {
//	    throw new IllegalStateException("Transformation of EA query based source not yet implemented!");
//	}

	if (originalSource instanceof TypeBasedSource1) {
	    final TypeBasedSource1 source = (TypeBasedSource1) originalSource;
	    return new TypeBasedSource2(source.getEntityMetadata(), originalSource.getAlias(), source.getDomainMetadataAnalyser());
	} else {
	    final QueryBasedSource1 source = (QueryBasedSource1) originalSource;
		final List<EntQuery2> transformed = new ArrayList<>();
		for (final EntQuery1 entQuery :source.getModels()) {
		    transformed.add(entQuery.transform(produceNewOne()));
		}

	    return new QueryBasedSource2(originalSource.getAlias(), source.getDomainMetadataAnalyser(), transformed.toArray(new EntQuery2[]{}));
	}
    }

    private Map<ISource1<? extends ISource2>, SourceInfo> getCurrentQueryMap() {
	return sourceMap.get(0);
    }

    public ISource2 getTransformedSource(final ISource1<? extends ISource2> originalSource) {
	return getCurrentQueryMap().get(originalSource).source;
    }

    public EntProp2 getTransformedProp(final EntProp1 originalProp) {
	final Iterator<Map<ISource1<? extends ISource2>, SourceInfo>> it = sourceMap.iterator();
	if (originalProp.isExternal()) {
	    it.next();
	}

	for (; it.hasNext();) {
	    final Map<ISource1<? extends ISource2>, SourceInfo> item = it.next();
	    final PropResolution resolution = resolveProp(item.values(), originalProp);
	    if (resolution != null) {
		return generateTransformedProp(resolution);
	    }
	}

	throw new IllegalStateException("Can't resolve property [" + originalProp.getName() + "].");
    }

    private EntProp2 generateTransformedProp(final PropResolution resolution) {
//	System.out.println("         ---+ " + resolution.resolution);
	final AbstractPropInfo propInfo = resolution.resolution;
	final Expression2 expr = propInfo.getExpression() != null ? propInfo.getExpression().transform(this.produceOneForCalcPropExpression(resolution.source)) : null;
	return new EntProp2(resolution.entProp.getName(), resolution.source, resolution.aliased, resolution.resolution, expr);
    }

    public static class PropResolution {
	private final boolean aliased;

	public PropResolution(final boolean aliased, final ISource2 source, final AbstractPropInfo resolution, final EntProp1 entProp) {
	    super();
	    this.aliased = aliased;
	    this.source = source;
	    this.resolution = resolution;
	    this.entProp = entProp;
	}

	private final ISource2 source;
	private final AbstractPropInfo resolution;
	private final EntProp1 entProp;
    }

    private PropResolution resolvePropAgainstSource(final SourceInfo source, final EntProp1 entProp) {
	final AbstractPropInfo asIsResolution = source.entityInfo.resolve(entProp.getName());
	if (source.source.getAlias() != null && source.aliasingAllowed && entProp.getName().startsWith(source.source.getAlias() + ".")) {
	    final String aliasLessPropName = entProp.getName().substring(source.source.getAlias().length() + 1);
	    final AbstractPropInfo aliasLessResolution = source.entityInfo.resolve(aliasLessPropName);
	    if (aliasLessResolution != null) {
		if (asIsResolution == null) {
		    return new PropResolution(true, source.source, aliasLessResolution, entProp);
		} else {
		    throw new IllegalStateException("Ambiguity while resolving prop [" + entProp.getName() + "]. Both [" + entProp.getName() + "] and [" + aliasLessPropName
			    + "] are resolvable against given source.");
		}
	    }
	}
	return asIsResolution != null ? new PropResolution(false, source.source, asIsResolution, entProp) : null;
    }

    private PropResolution resolveProp(final Collection<SourceInfo> sources, final EntProp1 entProp) {
	final List<PropResolution> result = new ArrayList<>();
//	System.out.println("-======== " + entProp);
	for (final SourceInfo pair : sources) {
//	    System.out.println("-============== pair is " + pair);
//	    System.out.println("-============== pair key source type is " + pair.source.sourceType());
//	    System.out.println("-====================== " + pair.source.sourceType().getSimpleName() + " : " + pair.entityInfo);
	    final PropResolution resolution = resolvePropAgainstSource(pair, entProp);
	    if (resolution != null) {
		result.add(resolution);
	    }
	}

	if (result.size() > 1) {
	    throw new IllegalStateException("Ambiguity while resolving prop [" + entProp.getName() + "]");
	}

	return result.size() == 1 ? result.get(0) : null;
    }

    public String generateNextSqlParamName() {
	return null;
    }
}