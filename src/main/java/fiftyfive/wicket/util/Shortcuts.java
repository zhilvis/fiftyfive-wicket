/**
 * Copyright 2011 55 Minutes (http://www.55minutes.com)
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
import java.util.Collection;

import fiftyfive.util.Assert;
import fiftyfive.util.ReflectUtils;
import fiftyfive.wicket.basic.LabelWithPlaceholder;
import fiftyfive.wicket.css.InternetExplorerCss;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Response;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.string.Strings;

/**
 * Helper methods that greatly reduce the amount of boilerplate code needed
 * for common Wicket tasks. Consider adding this
 * in the Java file of your wicket page or component:
 * <pre class="example">
 * import static fiftyfive.wicket.util.Shortcuts.*;</pre>
 *
 * @author Matt Brictson
 */
public class Shortcuts
{
    private static final IBehavior EMPTY_BEHAVIOR = new AbstractBehavior() {};
    
    /**
     * Shortcut for creating a PropertyModel. Equivalent to:
     * <pre class="example">
     * new PropertyModel(bean, propertyExpr)</pre>
     * 
     * @see PropertyModel
     */
    public static PropertyModel prop(Object bean, String propertyExpr)
    {
        return new PropertyModel(bean, propertyExpr);
    }
    
    /**
     * Creates a model that loads itself by exeucting the specified method
     * on a bean. The return value of that method will be cached as the value
     * of the model. When the model is detached, the cache will be discarded.
     * This is useful for connecting your Wicket page to your back-end:
     * <pre class="example">
     * public PersonDetailPage(PageParameters params)
     * {
     *     super(params);
     *     setDefaultModel(loadedModel(this, "loadPerson"));
     * }
     * private Person loadPerson()
     * {
     *     return _personService.loadPerson(getPageParameters().get("id"));
     * }</pre>
     * 
     * @throws IllegalArgumentException if the loadMethod does not exist, or
     *                                  takes more than zero arguments
     */
    public static LoadableDetachableModel loadedModel(final Object bean,
                                                      final String loadMethod)
    {
        // assert that method exists
        ReflectUtils.getZeroArgMethod(bean.getClass(), loadMethod);
        
        return new LoadableDetachableModel() {
            @Override protected Object load()
            {
                return ReflectUtils.invokeZeroArgMethod(bean, loadMethod);
            }
        };
    }
    
    /**
     * Shortcut for creating a LabelWithPlaceholder with an implied model.
     * Equivalent to:
     * <pre class="example">
     * new LabelWithPlaceholder("name")</pre>
     * 
     * @see LabelWithPlaceholder
     */
    public static LabelWithPlaceholder label(String id)
    {
        return new LabelWithPlaceholder(id);
    }

    /**
     * Shortcut for creating a LabelWithPlaceholder with a PropertyModel.
     * Equivalent to:
     * <pre class="example">
     * new LabelWithPlaceholder("name", new PropertyModel(person, "fullName"))</pre>
     * 
     * @see LabelWithPlaceholder
     */
    public static LabelWithPlaceholder label(String id,
                                             Object bean,
                                             String propertyExpr)
    {
        return label(id, prop(bean, propertyExpr));
    }

    /**
     * Shortcut for creating a LabelWithPlaceholder with a hardcoded value or a
     * custom model. Equivalent to:
     * <pre class="example">
     * new LabelWithPlaceholder("name", "Hardcoded string value")</pre>
     * 
     * @see LabelWithPlaceholder
     */
    public static LabelWithPlaceholder label(String id,
                                             Serializable valueOrIModel)
    {
        IModel model = valueOrIModel != null ? new Model(valueOrIModel) : null;
        if(valueOrIModel instanceof IModel)
        {
            model = (IModel) valueOrIModel;
        }
        return new LabelWithPlaceholder(id, model);
    }
    
    /**
     * Appends the specified text after the
     * closing tag of the component it decorates. The text will <b>not</b> be
     * escaped to be safe HTML, so take care to escape the string first if
     * necessary.
     * <p>
     * This shortcut is very useful for doing comma separated lists.
     * For example:
     * <pre class="example">
     * add(new ListView("list", myList) {
     *     &#064;Override protected void populateItem(ListItem item)
     *     {
     *         if(item.getIndex() &lt; getList().size() - 1)
     *         {
     *             item.add(afterTag(", "));
     *         }
     *     }
     * });</pre>
     */
    public static IBehavior afterTag(final String textToAppend)
    {
        return new AbstractBehavior() {
            @Override
            public void onRendered(Component component)
            {
                Response response = component.getResponse();
                response.write(textToAppend);
            }
        };
    }
    
    /**
     * Adds a CSS class to the component it decorates.
     * Equivalent to:
     * <pre class="example">
     * new AttributeAppender("class", true, new Model(cssClass), " ");</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssClass(IModel<String> cssClass)
    {
        if(null == cssClass)
        {
            return EMPTY_BEHAVIOR;
        }
        return new AttributeAppender("class", true, cssClass, " ") {
            @Override
            protected String newValue(String current, String append)
            {
                if(null == current && null == append) return null;
                return super.newValue(current, append);
            }
        };
    }

    /**
     * Adds a CSS class to the component it decorates.
     * Equivalent to:
     * <pre class="example">
     * new AttributeAppender("class", true, new Model(cssClass), " ");</pre>
     */
    public static IBehavior cssClass(String cssClass)
    {
        return cssClass(new Model(cssClass));
    }
    
    /**
     * Creates a header contributor that adds a &lt;link&gt; to a CSS file with
     * the same name as the specified class. For example:
     * <pre class="example">
     * add(cssResource(MyPanel.class));</pre>
     * <p>
     * will add a &lt;link&gt; to the &lt;head&gt; for {@code MyPanel.css},
     * found in the same classpath location as {@code MyPanel.class}.
     * <p>
     * This is equivalent to:
     * <pre class="example">
     * CSSPackageResource.getHeaderContribution(
     *     new CompressedResourceReference(
     *         cls, Classes.simpleName(cls) + ".css"
     *     )
     * );</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssResource(Class<?> cls)
    {
        Assert.notNull(cls);
        return CSSPackageResource.getHeaderContribution(
            new CompressedResourceReference(
                cls, Classes.simpleName(cls) + ".css"
            )
        );
    }
        
    /**
     * Creates a header contributor that adds a &lt;link&gt; to a CSS file with
     * the specified name, relative to the current application class.
     * For example:
     * <pre class="example">
     * add(cssResource("screen.css"));</pre>
     * <p>
     * will add a &lt;link&gt; to the &lt;head&gt; for {@code screen.css},
     * found in the same classpath location as your wicket application class.
     * <p>
     * This is equivalent to:
     * <pre class="example">
     * CSSPackageResource.getHeaderContribution(
     *     new CompressedResourceReference(
     *         Application.get().getClass(), "screen.css"
     *     )
     * );</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssResource(String filename)
    {
        Assert.notNull(filename);
        return CSSPackageResource.getHeaderContribution(
            new CompressedResourceReference(
                Application.get().getClass(), filename
            )
        );
    }

    /**
     * Creates a header contributor that adds a &lt;link&gt; to a CSS file with
     * the specified name, relative to the given class.
     * For example:
     * <pre class="example">
     * add(cssResource(BasePage.class, "screen.css"));</pre>
     * <p>
     * will add a &lt;link&gt; to the &lt;head&gt; for {@code screen.css},
     * found in the same classpath location as BasePage.
     * <p>
     * This is equivalent to:
     * <pre class="example">
     * CSSPackageResource.getHeaderContribution(
     *     new CompressedResourceReference(
     *         BasePage.class, "screen.css"
     *     )
     * );</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssResource(Class<?> scope, String filename)
    {
        Assert.notNull(scope, "scope cannot be null");
        Assert.notNull(filename, "filename cannot be null");
        return CSSPackageResource.getHeaderContribution(
            new CompressedResourceReference(scope, filename)
        );
    }
        
    /**
     * Creates a header contributor that adds a &lt;link&gt; to a print
     * stylesheet CSS file with the specified name, relative to the current
     * application class.
     * For example:
     * <pre class="example">
     * add(cssPrintResource("print.css"));</pre>
     * <p>
     * will add a &lt;link&gt; to the &lt;head&gt; for {@code print.css},
     * found in the same classpath location as your wicket application class.
     * The &lt;link&gt; will have a print media type.
     * <p>
     * This is equivalent to:
     * <pre class="example">
     * CSSPackageResource.getHeaderContribution(
     *     new CompressedResourceReference(
     *         Application.get().getClass(), "print.css"
     *     ),
     *     "print"
     * );</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssPrintResource(String filename)
    {
        Assert.notNull(filename);
        return CSSPackageResource.getHeaderContribution(
            new CompressedResourceReference(
                Application.get().getClass(), filename
            ),
            "print"
        );
    }

    /**
     * Creates a header contributor that adds a &lt;link&gt; to an
     * Internet Explorer conditional stylesheet CSS file with
     * the specified name, relative to the current application class.
     * The stylesheet will apply based on the IE condition argument.
     * For example:
     * <pre class="example">
     * add(cssConditionalResource("IE 7", "ie-7.css"));</pre>
     * <p>
     * will add a &lt;link&gt; to the &lt;head&gt; for {@code ie-7.css},
     * found in the same classpath location as your wicket application class.
     * The stylesheet will only be loaded in IE 7 browsers.
     * <p>
     * This is equivalent to:
     * <pre class="example">
     * InternetExplorerCss.getConditionalHeaderContribution(
     *     "IE 7"
     *     new CompressedResourceReference(
     *         Application.get().getClass(), "ie-7.css")
     *     )
     * );</pre>
     * 
     * @since 2.0
     */
    public static IBehavior cssConditionalResource(String cond, String filename)
    {
        Assert.notNull(cond, "condition cannot be null");
        Assert.notNull(filename, "filename cannot be null");
        return InternetExplorerCss.getConditionalHeaderContribution(
            cond,
            new CompressedResourceReference(
                Application.get().getClass(), filename
            )
        );
    }
    
    /**
     * Return true if the object is null, zero-length (if array or collection)
     * false (if a boolean) or blank (if a string). Examples:
     * <table>
     *   <tr><th>Object</th><th>Empty?</th></tr>
     *   <tr><td><code>"foo"</code></td><td><code>false</code></td></tr>
     *   <tr><td><code>""</code></td><td><code>true</code></td></tr>
     *   <tr><td><code>"   "</code></td><td><code>true</code></td></tr>
     *   <tr><td><code>null</code></td><td><code>true</code></td></tr>
     *   <tr><td><code>Collections.EMPTY_LIST</code></td><td><code>true</code></td></tr>
     *   <tr><td><code>Collections.singletonList("foo")</code></td><td><code>false</code></td></tr>
     *   <tr><td><code>new String[0]</code></td><td><code>true</code></td></tr>
     *   <tr><td><code>new String[] { "foo" }</code></td><td><code>false</code></td></tr>
     *   <tr><td><code>true</code></td><td><code>false</code></td></tr>
     *   <tr><td><code>false</code></td><td><code>true</code></td></tr>
     * </table>
     */
    public static boolean empty(Object obj)
    {
        boolean empty = false;
        if(null == obj)
        {
            empty = true;
        }
        else if(obj instanceof Boolean)
        {
            empty = ! (Boolean) obj;
        }
        else if(obj instanceof Collection)
        {
            empty = ((Collection) obj).size() == 0;
        }
        else if(obj instanceof String)
        {
            empty = Strings.isEmpty((String) obj);
        }
        else if(obj.getClass().isArray())
        {
            empty = ((Object[])obj).length == 0;
        }
        return empty;
    }
    
    /**
     * This class cannot be instantiated or subclassed.
     */
     private Shortcuts()
     {
     }
}
