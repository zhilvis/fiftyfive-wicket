/*
 * Copyright 2010 55 Minutes (http://www.55minutes.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fiftyfive.wicket.util;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.PropertyResolver;

/**
 * Applies the DRY principle to Wicket bookmarkable links and page parameters.
 * ParameterSpec lets you define your properties and parameters once; it
 * then takes care of link construction and page parameter parsing so you
 * don't have to do tedious PageParameters.put() and get() calls.
 * <p>
 * For pages that require only a single parameter this class is overkill.
 * However it is appropriate for detail pages or search
 * pages where there are many parameters that have to be parsed
 * from the URL in order to construct the page. In short, the ParameterSpec
 * does two things:
 * <ol>
 * <li>Given a model bean, ParameterSpec can construct a bookmarkable link with
 *     parameters taken from property values of that bean. For example, if
 *     your detail page requires an "id" and a "slug", this will pull
 *     <code>getId()</code> and <code>getSlug()</code> from your model bean
 *     on the fly to construct the link.</li>
 * <li>Going the other direction, ParameterSpec can parse the PageParameters
 *     of a page request and populate a bean with the appropriate properties.
 *     So if the URL contained "1" for the id parameter and "foo" for the
 *     slug parameter, your DetailBean would have its <code>setId()</code>
 *     and <code>setSlug()</code> call with those values. You could then send
 *     that bean to the backend for loading, etc.</li>
 * </ol>
 * Recommended usage: define a <code>SPEC</code> static
 * instance on your page.
 * <pre>
 * public class DetailPage extends WebPage
 * {
 *    public static final ParameterSpec SPEC = new ParameterSpec(
 *        DetailPage.class, "id", "slug"
 *    );
 *    ...
 * }
 * </pre>
 * Now when other parts of the application want add a link to this page, it is
 * as easy as this:
 * <pre>
 * add(DetailPage.SPEC.createLink("link", model));
 * </pre>
 *
 * @author Matt Brictson
 */
public class ParameterSpec<T> implements Serializable
{
    private Class <? extends WebPage> _pageClass;
    private Map<String,String>        _mapping = new HashMap();
    
    /**
     * Construct a ParameterSpec that will build links to the specified WebPage.
     * If any String arguments are specified, they will be used as property
     * expressions for extracting parameter values. It is assumed that the
     * property expression and PageParameters key are identical.
     * <p>
     * For example, if your PersonPage is mounted at "/person" and you want
     * URLs to look like "/person/[id]/[slug]", where id and slug are provided
     * by <code>PersonBean.getId()</code> and <code>PersonBean.getSlug()</code>,
     * you would mount the page using
     * {@link org.apache.wicket.request.target.coding.MixedParamUrlCodingStrategy}
     * and then construct the ParameterSpec like this:
     * <pre>
     * new ParameterSpec(PersonPage.class, "id", "slug");
     * </pre>
     * Which is the same as:
     * <pre>
     * ParameterSpec spec = new ParameterSpec(PersonPage.class);
     * spec.registerParameter("id", "id");
     * spec.registerParameter("slug", "slug");
     * </pre>
     *
     * @param page The target page of links created by this ParameterSpec
     * @param expressions Bean property expressions (e.g. "slug", "id") that
     *                    will be used to populate URLs to the page
     */
    public ParameterSpec(Class <? extends WebPage> page, String... expressions)
    {
        _pageClass = page;
        for(int i=0; i<expressions.length; i++)
        {
            registerParameter(expressions[i], expressions[i]);
        }
    }
    
    /**
     * Register a parameter that is required by bookmarkable links to your
     * page. For example, if your URL is "/search?q=[terms]" and the
     * terms come from a <code>getTerms()</code> property of your model bean,
     * then you would use this code:
     * <pre>
     * registerParameter("q", "terms")
     * </pre>
     * @param parameter The page parameter key (this may appear in the URL
     *                  depending on the coding strategy)
     * @param propExpr Bean property that will be used to populate URLs
     *                 when building links
     */
    public ParameterSpec registerParameter(String parameter, String propExpr)
    {
        _mapping.put(parameter, propExpr);
        return this;
    }
    
    /**
     * Creates a BookmarkablePageLink to the page managed by this ParameterSpec.
     * The link will have parameters dictated by the ParameterSpec constructor
     * or <code>registerParameter()</code> calls. The values of those
     * parameters will be taken from properties of the specified model bean.
     * For example, the link may require "id" and "slug" values, meaning
     * <code>getId()</code> and <code>getSlug()</code> will be called on the
     * model object at render time to populate the parameters of the link.
     *
     * @param id The wicket:id of the link in the HTML markup
     * @param model A model representing the bean that will be used to
     *              populate the parameters of the link
     */
    public BookmarkablePageLink createLink(String id, final IModel<T> model)
    {
        BookmarkablePageLink bpl = new BookmarkablePageLink(id, _pageClass) {
            @Override protected void onBeforeRender()
            {
                PageParameters params = createParameters(model.getObject());
                for(String key : (Set<String>) params.keySet())
                {
                    setParameter(key, params.getString(key));
                }
                super.onBeforeRender();
            }
        };
        return bpl;
    }
    
    /**
     * Creates a PageParameters map populated with the parameters dictated
     * in the ParameterSpec constructor or <code>registerParameter()</code>
     * calls. For each parameter, the property expression will be
     * evaluated against the given bean to retreive the value. For example,
     * if "id" is one of the expressions, <code>bean.getId()</code> will be
     * used to retrieve its value and place it in the PageParameters.
     */
    public PageParameters createParameters(T bean)
    {
        PageParameters params = new PageParameters();
        for(String key : _mapping.keySet())
        {
            String expression = _mapping.get(key);
            Object value = PropertyResolver.getValue(expression, bean);
            if(value != null)
            {
                params.put(key, value.toString());
            }
        }
        return params;
    }
    
    /**
     * Use this method in your page constructor to parse the
     * PageParameters. The specified bean will be populated by calling the
     * appropriate setters as defined by this ParameterSpec. For example, if
     * the ParameterSpec has been created parameters that map "id" and "slug"
     * properties, the <code>setId()</code> and <code>setSlug()</code>
     * methods of the bean will be called with values taken from the
     * PageParameters.
     *
     * @param params Values will be taken from these PageParameters
     * @param beanToPopulate Values will be set using appropriate setters on
     *                       this bean
     */
    public void parseParameters(PageParameters params, T beanToPopulate)
    {
        for(String key : _mapping.keySet())
        {
            String expr = _mapping.get(key);
            Object value = params.getString(key);
            if(value != null)
            {
                PropertyResolver.setValue(expr, beanToPopulate, value, null);
            }
        }
    }
}