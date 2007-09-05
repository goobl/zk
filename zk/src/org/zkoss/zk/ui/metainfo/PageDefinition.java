/* PageDefinition.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Tue May 31 11:27:07     2005, Created by tomyeh
}}IS_NOTE

Copyright (C) 2005 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zk.ui.metainfo;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.zkoss.lang.Classes;
import org.zkoss.lang.Objects;
import org.zkoss.util.resource.Locator;
import org.zkoss.xel.ExpressionFactory;
import org.zkoss.xel.Expressions;
import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.taglib.Taglibs;
import org.zkoss.xel.taglib.Taglib;
import org.zkoss.xml.HTMLs;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.util.Condition;
import org.zkoss.zk.ui.util.Initiator;
import org.zkoss.zk.scripting.Namespace;
import org.zkoss.zk.scripting.Namespaces;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.sys.PageCtrl;
import org.zkoss.zk.ui.sys.PageConfig;
import org.zkoss.zk.xel.Evaluator;
import org.zkoss.zk.xel.ExValue;
import org.zkoss.zk.xel.impl.SimpleEvaluator;
import org.zkoss.zk.xel.impl.EvaluatorRef;

/**
 * A page definition.
 * It represents a ZUL page.
 *
 * <p>Note: it is not thread-safe.
 *
 * <p>Note: it is not serializable.
 *
 * @author tomyeh
 * @see ComponentDefinition
 */
public class PageDefinition extends NodeInfo {
	private final LanguageDefinition _langdef;
	private final Locator _locator;
	private String _id, _title, _style;
	/** The request path. */
	private String _path = "";
	/** The zscript language. */
	private String _zslang = "Java";
	private List _taglibs;
	/** A map of imported classes for expression, Map<String nm, Class cls>. */
	private Map _expimps;
	/** The evaluator. */
	private Evaluator _eval;
	/** The evaluator reference. */
	private EvaluatorRef _evalr;
	/** The function mapper. */
	private FunctionMapper _mapper;
	/* List(InitiatorInfo). */
	private List _initdefs;
	/** List(VariableResolverInfo). */
	private List _resolvdefs;
	/** List(HeaderInfo). */
	private List _headerdefs;
	/** Map(String name, ExValue value). */
	private Map _rootAttrs;
	private ExValue _contentType, _docType, _firstLine;
	/** The expression factory (ExpressionFactory).*/
	private Class _expfcls;
	private final ComponentDefinitionMap _compdefs;
	private Boolean _cacheable;

	/** Constructor.
	 * @param langdef the default language which is used if no namespace
	 * is specified. Note: a page might have components from different
	 * languages.
	 */
	public PageDefinition(LanguageDefinition langdef, Locator locator) {
		if (langdef == null)
			throw new IllegalArgumentException("null langdef");
		if (locator == null)
			throw new IllegalArgumentException("null locator");

		_langdef = langdef;
		_locator = locator;
		_compdefs = new ComponentDefinitionMap(
			_langdef.getComponentDefinitionMap().isCaseInsensitive());
	}
	/** Constructor.
	 * @param langdef the default language which is used if no namespace
	 * is specified. Note: a page might have components from different
	 * languages.
	 * @param id the identifier. See {@link #setId}.
	 * @param title the title. See {@link #setTitle}.
	 * @param style the CSS style. See {@link #setStyle}.
	 */
	public PageDefinition(LanguageDefinition langdef, String id, String title,
	String style, Locator locator) {
		this(langdef, locator);
		setId(id);
		setTitle(title);
		setStyle(style);
	}

	/** Returns the language definition that this page is default to be.
	 */
	public LanguageDefinition getLanguageDefinition() {
		return _langdef;
	}
	/** Returns the parent (always null).
	 */
	public NodeInfo getParent() {
		return null;
	}

	/** Returns the locator associated with this page definition.
	 */
	public Locator getLocator() {
		return _locator;
	}

	/** Returns the default scripting language which is assumed when
	 * a zscript element doesn't specify any language.
	 *
	 * <p>Default: Java.
	 */
	public String getZScriptLanguage() {
		return _zslang;
	}
	/** Sets the default scripting language which is assumed when
	 * a zscript element doesn't specify any language.
	 *
	 * @param zslang the default scripting language.
	 */
	public void setZScriptLanguage(String zslang) {
		if (zslang == null || zslang.length() == 0)
			throw new IllegalArgumentException("null or empty");
		_zslang = zslang;
	}

	/** Returns the identitifer that will be assigned to pages created from
	 * this definition, or null if the identifier shall be generated automatically.
	 * <p>Note: the returned value might contain EL expressions.
	 */
	public String getId() {
		return _id;
	}
	/** Sets the identifier that will be assigned to pages created from this
	 * definition.
	 * @param id the identifier. It might contain EL expressions.
	 * If null or empty (null is assumed), page's ID is generated automatically.
	 * If not empty, ID (after evaluated) must be unquie in the same request.
	 */
	public void setId(String id) {
		_id = id != null && id.length() > 0 ? id: null;
	}
		
	/** Returns the title that will be assigned to pages created from
	 * this definition, or null if no title is assigned at the beginning.
	 * <p>Note: the returned value might contain EL expressions.
	 */
	public String getTitle() {
		return _title;
	}
	/** Sets the title that will be assigned to pages created from
	 * this definition, or null if no title is assigned at the beginning.
	 * @param title the title. If empty, null is assumed.
	 */
	public void setTitle(String title) {
		_title = title != null && title.length() > 0 ? title: null;
	}

	/** Returns the CSS style that will be assigned to pages created from
	 * this definition, or null if no style is assigned at the beginning.
	 * <p>Note: the returned value might contain EL expressions.
	 */
	public String getStyle() {
		return _style;
	}
	/** Sets the CSS style that will be assigned to pages created from
	 * this definition, or null if no style is assigned at the beginning.
	 * @param style the CSS style. If empty, null is assumed.
	 */
	public void setStyle(String style) {
		_style = style != null && style.length() > 0 ? style: null;
	}

	/** Returns the request path of this page definition, or ""
	 * if not available.
	 * <p>It is the same as the servlet path
	 * (javax.servlet.http.HttpServletRequest's getServletPath), if ZK is running
	 * at a servlet container.
	 */
	public String getRequestPath() {
		return _path;
	}
	/** Sets the request path of this page definition.
	 */
	public void setRequestPath(String path) {
		_path = path != null ? path: "";
	}

	/** Imports the component definitions from the specified definition.
	 */
	public void imports(PageDefinition pgdef) {
		if (_initdefs != null)
			for (Iterator it = pgdef._initdefs.iterator(); it.hasNext();)
				addInitiatorInfo((InitiatorInfo)it.next());

		for (Iterator it = pgdef._compdefs.getNames().iterator();
		it.hasNext();) {
			final String name = (String)it.next();
			addComponentDefinition(pgdef._compdefs.get(name));
		}
	}

	/** Adds a defintion of {@link org.zkoss.zk.ui.util.Initiator}. */
	public void addInitiatorInfo(InitiatorInfo init) {
		if (init == null)
			throw new IllegalArgumentException("null");

		if (_initdefs == null)
			_initdefs = new LinkedList();
		_initdefs.add(init);
	}
	/** Returns a list of all {@link Initiator} and invokes
	 * its {@link Initiator#doInit} before returning.
	 * It never returns null.
	 */
	public List doInit(Page page) {
		if (_initdefs == null)
			return Collections.EMPTY_LIST;

		final List inits = new LinkedList();
		for (Iterator it = _initdefs.iterator(); it.hasNext();) {
			final InitiatorInfo def = (InitiatorInfo)it.next();
			try {
				final Initiator init = def.newInitiator(this, page);
				if (init != null) {
					init.doInit(page, def.getArguments(this, page));
					inits.add(init);
				}
			} catch (Throwable ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
		return inits;
	}

	/** Adds a defintion of {@link VariableResolver}. */
	public void addVariableResolverInfo(VariableResolverInfo resolver) {
		if (resolver == null)
			throw new IllegalArgumentException("null");

		if (_resolvdefs == null)
			_resolvdefs = new LinkedList();
		_resolvdefs.add(resolver);
	}
	/** Initializes XEL context for the specified page.
	 */
	public void initXelContext(Page page) {
		page.addFunctionMapper(getFunctionMapper());

		if (_resolvdefs != null)
			for (Iterator it = _resolvdefs.iterator(); it.hasNext();) {
				final VariableResolverInfo def = (VariableResolverInfo)it.next();
				try {
					VariableResolver resolver =
						def.newVariableResolver(this, page);
					if (resolver != null) 
						page.addVariableResolver(resolver);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
				}
			}
	}

	/** Adds a header definition ({@link HeaderInfo}).
	 */
	public void addHeaderInfo(HeaderInfo header) {
		if (header == null)
			throw new IllegalArgumentException("null");

		if (_headerdefs == null)
			_headerdefs = new LinkedList();
		_headerdefs.add(header);
	}
	/** Converts the header definitions (added by {@link #addHeaderInfo}) to
	 * HTML tags.
	 */
	public String getHeaders(Page page) {
		if (_headerdefs == null)
			return "";

		final StringBuffer sb = new StringBuffer(256);
		for (Iterator it = _headerdefs.iterator(); it.hasNext();)
			sb.append(((HeaderInfo)it.next()).toHTML(this, page)).append('\n');
		return sb.toString();
	}

	/** Returns the content type (after evaluation),
	 * or null to use the device default.
	 *
	 * @param page the page used to evaluate EL expressions, if any
	 * @since 3.0.0
	 */
	public String getContentType(Page page) {
		return _contentType != null ?
			(String)_contentType.getValue(_evalr, page): null;
	}
	/** Sets the content type.
	 *
	 * <p>Default: null (use the device default).
	 *
	 * @param contentType the content type. It may coontain EL expressions.
	 * @since 3.0.0
	 */
	public void setContentType(String contentType) {
		_contentType = contentType != null && contentType.length() > 0 ?
			new ExValue(contentType, String.class): null;
	}
	/** Returns the doc type (&lt;!DOCTYPE&gt;) (after evaluation),
	 * or null to use the device default.
	 *
	 * @param page the page used to evaluate EL expressions, if any
	 * @since 3.0.0
	 */
	public String getDocType(Page page) {
		return _docType != null ?
			(String)_docType.getValue(_evalr, page): null;
	}
	/** Sets the doc type (&lt;!DOCTYPE&gt;).
	 *
	 * <p>Default: null (use the device default).
	 *
	 * @param docType the doc type. It may coontain EL expressions.
	 * @since 3.0.0
	 */
	public void setDocType(String docType) {
		_docType = docType != null && docType.length() > 0 ?
			new ExValue(docType, String.class): null;
	}
	/** Returns the first line to be generated to the output
	 * (after evaluation), or null if nothing to generate.
	 *
	 * <p>For XML devices, it is usually the xml processing instruction:<br/>
	 * <code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
	 *
	 * @param page the page used to evaluate EL expressions, if any
	 * @since 3.0.0
	 */
	public String getFirstLine(Page page) {
		return _firstLine != null ?
			(String)_firstLine.getValue(_evalr, page): null;
	}
	/** Sets the first line to be generated to the output.
	 *
	 * <p>Default: null (i.e., nothing generated)
	 * @since 3.0.0
	 */
	public void setFirstLine(String firstLine) {
		_firstLine = firstLine != null && firstLine.length() > 0 ?
			new ExValue(firstLine, String.class): null;
	}
	/** Returns if the client can cache the rendered result, or null
	 * to use the device default.
	 *
	 * @since 3.0.0
	 */
	public Boolean getCacheable() {
		return _cacheable;
	}
	/** Sets if the client can cache the rendered result.
	 *
	 * <p>Default: null (use the device default).
	 * @since 3.0.0
	 */
	public void setCacheable(Boolean cacheable) {
		_cacheable = cacheable;
	}

	/** Adds a root attribute.
	 * The previous attributee of the same will be replaced.
	 *
	 * @param value the value. If null, the attribute is removed.
	 * It can be an EL expression.
	 * @since 3.0.0
	 */
	public void setRootAttribute(String name, String value) {
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException();

		if (_rootAttrs == null) {
			if (value == null)
				return; //nothing to
			_rootAttrs = new LinkedHashMap();
		}

		if (value == null) _rootAttrs.remove(name);
		else _rootAttrs.put(name, new ExValue(value, String.class));
	}
	/** Converts the header definitions (added by {@link #setRootAttribute})
	 * to the attributes of the root element.
	 * For HTML, the root element is the HTML element.
	 * @since 3.0.0
	 */
	public String getRootAttributes(Page page) {
		if (_rootAttrs == null || _rootAttrs.isEmpty())
			return "";

		final Evaluator eval = getEvaluator();
		final StringBuffer sb = new StringBuffer(256);
		for (Iterator it = _rootAttrs.entrySet().iterator(); it.hasNext();) {
			final Map.Entry me = (Map.Entry)it.next();
			final String val = (String)
				((ExValue)me.getValue()).getValue(eval, page);
			if (val != null)
				HTMLs.appendAttribute(sb, (String)me.getKey(), val);
		}
		return sb.toString();
	}

	/** Returns the map of component definition (never null).
	 */
	public ComponentDefinitionMap getComponentDefinitionMap() {
		return _compdefs;
	}
	/** Adds a component definition belonging to this page definition only.
	 *
	 * <p>It is the same as calling {@link ComponentDefinitionMap#add} 
	 * against {@link #getComponentDefinitionMap}
	 */
	public void addComponentDefinition(ComponentDefinition compdef) {
		final LanguageDefinition langdef = compdef.getLanguageDefinition();
		if (langdef != null) {
			final LanguageDefinition ld2 = getLanguageDefinition();
			if (langdef != ld2
			&& !langdef.getDeviceType().equals(ld2.getDeviceType()))
				throw new UiException("Component definition, "+compdef
					+", does not belong to the same device type of the page definition, "
					+ld2.getDeviceType());
		}
		_compdefs.add(compdef);
	}
	/** Returns the component definition of the specified name, or null
	 * if not found.
	 *
	 * <p>Note: unlike {@link LanguageDefinition#getComponentDefinition},
	 * this method doesn't throw ComponentNotFoundException if not found.
	 * It just returns null.
	 *
	 * @param recur whether to look up the component from {@link #getLanguageDefinition}
	 */
	public ComponentDefinition getComponentDefinition(String name, boolean recur) {
		final ComponentDefinition compdef = _compdefs.get(name);
		if (!recur || compdef != null)
			return compdef;

		try {
			return getLanguageDefinition().getComponentDefinition(name);
		} catch (DefinitionNotFoundException ex) {
		}
		return null;
	}
	/** Returns the component definition of the specified class, or null
	 * if not found.
	 *
	 * <p>Note: unlike {@link LanguageDefinition#getComponentDefinition},
	 * this method doesn't throw ComponentNotFoundException if not found.
	 * It just returns null.
	 *
	 * @param recur whether to look up the component from {@link #getLanguageDefinition}
	 */
	public ComponentDefinition getComponentDefinition(Class cls, boolean recur) {
		final ComponentDefinition compdef = _compdefs.get(cls);
		if (!recur || compdef != null)
			return compdef;

		try {
			return getLanguageDefinition().getComponentDefinition(cls);
		} catch (DefinitionNotFoundException ex) {
		}
		return null;
	}

	/** Adds a tag lib. */
	public void addTaglib(Taglib taglib) {
		if (taglib == null)
			throw new IllegalArgumentException("null");

		if (_taglibs == null)
			_taglibs = new LinkedList();
		_taglibs.add(taglib);
		_eval = null; //ask for re-gen
		_mapper = null; //ask for re-parse
	}
	/** Adds an imported class to the expression factory.
	 * @since 3.0.0
	 */
	public void addExpressionImport(String nm, Class cls) {
		if (nm == null || cls == null)
			throw new IllegalArgumentException();
		if (_expimps == null)
			_expimps = new HashMap(4);
		_expimps.put(nm, cls);
		_eval = null; //ask for re-gen
		_mapper = null; //ask for re-parse
	}
	/** Sets the implementation of the expression factory that shall
	 * be used by this page.
	 *
	 * <p>Default: null (use the default).
	 *
	 * @param expfcls the implemtation class, or null to use the default.
	 * Note: expfcls must implement {@link ExpressionFactory}.
	 * If null is specified, the class defined in
	 * {@link org.zkoss.zk.ui.util.Configuration#getExpressionFactoryClass}
	 * @since 3.0.0
	 */
	public void setExpressionFactoryClass(Class expfcls) {
		if (expfcls != null && !ExpressionFactory.class.isAssignableFrom(expfcls))
			throw new IllegalArgumentException(expfcls+" must implement "+ExpressionFactory.class);
		_expfcls = expfcls;
	}
	/** Returns the implementation of the expression factory that
	 * is used by this page, or null if
	 * {@link org.zkoss.zk.ui.util.Configuration#getExpressionFactoryClass}
	 * is used.
	 *
	 * @see #setExpressionFactoryClass
	 * @since 3.0.0
	 */
	public Class getExpressionFactoryClass() {
		return _expfcls;
	}

	/** Returns the evaluator based on this page definition (never null).
	 * @since 3.0.0
	 */
	public Evaluator getEvaluator() {
		if (_eval == null)
			_eval = newEvaluator();
		return _eval;
	}
	private Evaluator newEvaluator() {
		return new SimpleEvaluator(getFunctionMapper(), _expfcls);
	}
	/** Returns the evaluator reference (never null).
	 * <p>This method is used only for implementation only.
	 * @since 3.0.0
	 */
	public EvaluatorRef getEvaluatorRef() {
		if (_evalr == null)
			_evalr = newEvaluatorRef();
		return _evalr;
	}
	private EvaluatorRef newEvaluatorRef() {
		return new PageEvalRef(this);
	}

	/** Returns the function mapper, or null if no mappter at all.
	 */
	public FunctionMapper getFunctionMapper() {
		if (_mapper == null) {
			_mapper = Taglibs.getFunctionMapper(_taglibs, _expimps, _locator);
			if (_mapper == null)
				_mapper = Expressions.EMPTY_MAPPER;
		}
		return _mapper != Expressions.EMPTY_MAPPER ? _mapper: null;
	}

	/** Initializes a page after execution is activated.
	 * It setup the identifier and title, and adds it to desktop.
	 */
	public void init(final Page page, final boolean evalHeaders) {
		((PageCtrl)page).init(
			new PageConfig() {
				public String getId() {return _id;}
				public String getUuid() {return null;}
				public String getTitle() {return _title;}
				public String getStyle() {return _style;}
				public String getHeaders() {
					return evalHeaders ?
						PageDefinition.this.getHeaders(page): "";
				}
				public String getRootAttributes() {
					return PageDefinition.this.getRootAttributes(page);
				}
				public String getContentType() {
					return PageDefinition.this.getContentType(page);
				}
				public String getDocType() {
					return PageDefinition.this.getDocType(page);
				}
				public String getFirstLine() {
					return PageDefinition.this.getFirstLine(page);
				}
				public Boolean getCacheable() {return _cacheable;}
			});
	}

	//NodeInfo//
	public PageDefinition getPageDefinition() {
		return this;
	}

	//Object//
	public String toString() {
		return "[PageDefinition:"+(_id != null ? _id: _title)+']';
	}
}
