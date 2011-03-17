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
package fiftyfive.wicket.js;

import fiftyfive.util.Assert;
import fiftyfive.wicket.js.locator.DependencyCollection;
import fiftyfive.wicket.js.locator.JavaScriptDependencyLocator;
import fiftyfive.wicket.resource.MergedResourceBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>This class is not yet compatible with Wicket 1.5.</b>
 * <p>
 * Instructs Wicket to merge a list of JavaScript resources into a single file
 * when the application is in deployment mode. Consider using this in your
 * application as a performance optimization.
 * <p>
 * Example usage:
 * <pre class="example">
 * public class MyApplication extends WebApplication
 * {
 *     &#064;Override
 *     protected void init()
 *     {
 *         super.init();
 * 
 *         new MergedJavaScriptBuilder()
 *             .setPath("/scripts/all.js")
 *             .addWicketAjaxLibraries()
 *             .addJQueryUI()
 *             .addLibrary("jquery.scrollTo")
 *             .addLibrary("jquery.55_utils")
 *             .addLibrary("55_utils")
 *             .addLibrary("strftime")
 *             .build(this);
 * 
 *         // The return value from build() can be used on your base page to
 *         // inject all these JavaScript resources in one shot, if desired.
 *     }
 * }</pre>
 * 
 * @since 2.0
 */
public class MergedJavaScriptBuilder extends MergedResourceBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(
        MergedJavaScriptBuilder.class
    );
    
    private DependencyCollection deps;
    
    /**
     * Creates an empty builder object. See the
     * {@link MergedJavaScriptBuilder class documentation} for 
     * example usage.
     */
    public MergedJavaScriptBuilder()
    {
        super();
        this.deps = new DependencyCollection();
    }
    
    public MergedJavaScriptBuilder setPath(String path)
    {
        return (MergedJavaScriptBuilder) super.setPath(path);
    }
    
    /**
     * Adds a JavaScript file to the list of merged resources. The
     * dependencies of the script, if declared using
     * <a href="http://getsprockets.org/>Sprockets</p> syntax within
     * the JS file, will also be added automatically.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addScript(Class<?> scope, String path)
    {
        getDependencyLocator().findResourceScripts(scope, path, this.deps);
        return this;
    }
    
    /**
     * Adds a JavaScript resource to the list of merged resources. The
     * dependencies of the script, if declared using
     * <a href="http://getsprockets.org/">Sprockets</a> syntax within
     * the JS file, will also be added automatically.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addScript(ResourceReference ref)
    {
        getDependencyLocator().findResourceScripts(
            ref.getScope(),
            ref.getName(),
            this.deps
        );
        return this;
    }
    
    /**
     * Adds a JavaScript resource of the same name and location of the given
     * class, except with the ".js" extension. These two statements are
     * equivalent:
     * <pre class="example">
     * addAssociatedScript(MyPanel.class);
     * addScript(MyPanel.class, "MyPanel.js");</pre>
     * <p>
     * The dependencies of the script, if declared using
     * <a href="http://getsprockets.org/">Sprockets</a> syntax within
     * the JS file, will also be added automatically.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addAssociatedScript(Class<?> cls)
    {
        getDependencyLocator().findAssociatedScripts(cls, this.deps);
        return this;
    }

    /**
     * Adds jQuery and jQuery UI to the list of merged resources.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addJQueryUI()
    {
        return addLibrary("jquery-ui");
    }
    
    /**
     * Adds Wicket's wicket-event.js and wicket-ajax.js files to the list
     * of merged resources.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addWicketAjaxLibraries()
    {
        addScript(WicketEventReference.INSTANCE);
        addScript(WicketAjaxReference.INSTANCE);
        return this;
    }
    
    /**
     * Adds a JavaScript library to the list of merged resources. The
     * dependencies of the script, if declared using
     * <a href="http://getsprockets.org/">Sprockets</a> syntax within
     * the JS file, will also be added automatically.
     *
     * @see JavaScriptDependencySettings
     */
    public MergedJavaScriptBuilder addLibrary(String libraryName)
    {
        Assert.notNull(libraryName);
        getDependencyLocator().findLibraryScripts(libraryName, this.deps);
        return this;
    }
    
    @Override
    public Behavior build(WebApplication app)
    {
        for(ResourceReference ref : this.deps)
        {
            LOGGER.debug("Added script to merged builder: {}", ref);
            add(ref);
        }
        return super.build(app);
    }
    
    protected Behavior newContributor(final ResourceReference ref)
    {
        return new Behavior() {
            @Override
            public void renderHead(Component comp, IHeaderResponse response)
            {
                response.renderJavaScriptReference(ref);
            }
        };
    }

    private JavaScriptDependencyLocator getDependencyLocator()
    {
        return JavaScriptDependencySettings.get().getLocator();
    }
}