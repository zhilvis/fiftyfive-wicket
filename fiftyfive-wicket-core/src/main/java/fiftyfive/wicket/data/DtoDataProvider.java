/**
 * Copyright 2012 55 Minutes (http://www.55minutes.com)
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
package fiftyfive.wicket.data;

import fiftyfive.util.ReflectUtils;
import java.io.Serializable;
import java.util.Iterator;
import org.apache.wicket.markup.repeater.AbstractPageableView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Checks;

/**
 * An IDataProvider that implements the DTO pattern. Suitable for full-text
 * search results and other result sets where size and data are returned in a
 * single DTO.
 * <p>
 * This is a drop-in replacement for {@link IDataProvider}, allowing you to use
 * Wicket's existing components like
 * {@link org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView DataGridView},
 * {@link org.apache.wicket.markup.repeater.data.DataView DataView},
 * {@link org.apache.wicket.markup.html.navigation.paging.PagingNavigator PagingNavigator},
 * etc. in a DTO-style efficient manner without any customization.
 * (However see the cautionary note below.)
 * <p>
 * The main advantage of using this class is that it implements
 * {@link IDataProvider#size IDataProvider.size()} and
 * {@link IDataProvider#iterator IDataProvider.iterator()}
 * with a single backend query. This is accomplished by maintaining
 * a reference to a pageable view, so that page size and offset can be
 * determined when {@code size()} is called.
 * The size is also cached to prevent
 * extra backend calls when paging links are clicked.
 * <p>
 * In other words, rather than having to issue two calls to the backend, once
 * to determine the size of the result, and then again to determine the actual
 * rows of data in the result, you can instead
 * {@link #load(int,int) implement a single load() method}
 * that returns a DTO containing both the size and the data. Often
 * times this is much more efficient, especially when dealing with web service
 * and full-text search implementations.
 * <p>
 * <b>Be sure to call {@link #flushSizeCache() flushSizeCache()} or construct
 * a new DtoDataProvider when you know your result size will
 * change, for example if the user changes her search criteria.</b>
 * <p>
 * Generic types:
 * <ul>
 * <li>{@code R} is a <b>R</b>esult DTO: a container class that holds the
 *     elements of actual data of the current page, plus a total size of the
 *     result.
 *     </li>
 * <li>{@code E} represents each <b>E</b>lement of data 
 *     in the result container.</li>
 * </ul>
 * <p>
 * Note that since DtoDataProvider needs a reference back to the pageable view
 * that is displaying its data, the object construction process takes a few
 * steps:
 * <pre class="example">
 * // Let's say this is our concrete implementation.
 * public class UserResultProvider extends DtoDataProvider&lt;UserSearchResult,User&gt;
 * {
 *     // implement iterator(UserSearchResult), size(UserSearchResult) and load(int,int)
 * }
 * 
 * // To use our provider to drive a DataView, first we construct our provider.
 * UserResultProvider provider = new UserResultProvider();
 * 
 * // Then construct the DataView, passing in our provider.
 * DataView&lt;User&gt; dataView = new DataView&lt;User&gt;("users", provider) {
 *     // implement populateItem()
 * };
 * 
 * // Finally, wire up our provider back to the view
 * provider.setPageableView(dataView);</pre>
 * <p>
 * <b>Caution: This class should be considered experimental.</b>
 * By implementing {@code size()} and {@code iterator()} with a single backend
 * query, this class goes against the Wicket developers' original intentions
 * for the IDataProvider interface. We accomplish this feat by
 * using the Java reflection API to access private data within
 * {@link AbstractPageableView}.
 * 
 * @since 2.0
 */
public abstract class DtoDataProvider<R,E> implements IDataProvider<E>
{
    private transient R transientResult;
    private transient Integer transientOffset;
    private transient Integer transientAmount;
    
    private Integer cachedDataSize;
    private AbstractPageableView pageableView;
    
    /**
     * Constructs an empty provider. You must call
     * {@link #setPageableView setPageableView()} before
     * the provider can be used.
     */
    public DtoDataProvider()
    {
        super();
    }
    
    /**
     * Constructs a provider that will use size and offset information from
     * the specified {@code AbstractPageableView} when loading data.
     */
    public DtoDataProvider(AbstractPageableView pageableView)
    {
        super();
        this.pageableView = pageableView;
    }
    
    /**
     * Flush the cached size information that is normally held between
     * requests. This method should be called for example when your search
     * criteria changes, meaning that the result data could completely change.
     * <p>
     * You shouldn't need to use this method, since new search
     * criteria would normally mean constructing a completely new
     * DtoDataProvider.
     */
    public void flushSizeCache()
    {
        this.cachedDataSize = null;
    }
    
    /**
     * Returns the pageable view associated with this provider.
     */
    public AbstractPageableView getPageableView()
    {
        return this.pageableView;
    }
    
    /**
     * Sets the {@code AbstractPageableView} for which this object will be used
     * as data provider. The pageable view is consulted whenever the result
     * object is loaded from the backend, in order to get the current page
     * offset and page size. This property must not be {@code null}.
     */
    public void setPageableView(AbstractPageableView pageableView)
    {
        this.pageableView = pageableView;
    }
    
    /**
     * Loads the result object from the backend. The object will be cached
     * for the remainder of the current request, or until
     * {@link #detach() detach()} is called.
     * 
     * @param offset A zero-based offset of the first result desired, based on
     *               the current page number and page size.
     * @param amount The number of results desired (i.e. the page size).
     */
    protected abstract R load(int offset, int amount);
    
    /**
     * Returns an iterator of the items contained in the given result object.
     */
    protected abstract Iterator<? extends E> iterator(R result);
    
    /**
     * Returns the total number of items in the entire result, as represented
     * by the given result object.
     */
    protected abstract int size(R result);

    // IDataProvider support
    
    /**
     * Loads the result DTO from the backend if necessary, then delegates
     * to the implementation of {@link #iterator(Object) iterator(R)}.
     */
    public Iterator<? extends E> iterator(long offset, long amount)
    {
        return iterator(getCachedResultOrLoad((int)offset, (int)amount));
    }
    
    /**
     * This implementation assumes the object is Serializable and simply
     * calls Model.of(). You may wish to override with a custom model.
     */
    public IModel<E> model(E object)
    {
        return (IModel<E>) Model.of((Serializable)object);
    }
    
    /**
     * Loads the result DTO from the back-end based on the current state
     * of the page. A cached version of the DTO will be used if possible
     * to reduce extra back-end calls.
     * <p>
     * Delegates to the implementation of
     * {@link #size(Object) size(R)}, which subclasses must implement.
     * <p>
     * This result will be cached, and the cache used if possible.
     */
    public long size()
    {
        if(null == this.cachedDataSize)
        {
            this.cachedDataSize = size(getCachedResultOrLoad());
        }
        return this.cachedDataSize;
    }
    
    // loadable detachable support
    
    /**
     * Loads and returns the result DTO from the backend, or returns the
     * cached copy if it has already been loaded. The cache is discarded
     * automatically if the cache is out of date (i.e. the offset and
     * amount to load have changed). The cache is also discarded when
     * {@link #detach() detach()} is called.
     * <p>
     * This no-argument version infers the offset and amount to load based
     * on the pageable view that is being used with this data provider.
     * 
     * @since 4.0
     */
    public R getCachedResultOrLoad()
    {
        return getCachedResultOrLoad((int)getPageableViewOffset(), (int)getPageableRowsPerPage());
    }
    
    /**
     * Loads and returns the result DTO from the backend, or returns the
     * cached copy if it has already been loaded. The cache is discarded
     * automatically if the cache is out of date (i.e. the offset and
     * amount to load have changed). The cache is also discarded when
     * {@link #detach() detach()} is called.
     * <p>
     * The explicit offset and amount parameters indicate the items to be
     * loaded. If the cache was for a different set of parameters, it will
     * be discarded.
     * 
     * @since 4.0
     */
    public R getCachedResultOrLoad(int offset, int amount)
    {
        if(isCacheStale(offset, amount))
        {
            // Reset cached values by loading from the back-end
            this.transientOffset = offset;
            this.transientAmount = amount;
            this.transientResult = load(offset, amount);
        }
        // Return the cached result
        return this.transientResult;
    }
    
    /**
     * Discards the cached view offset, rows per page, and result DTO objects.
     * Note that the result size remains cached.
     */
    public void detach()
    {
        this.transientResult = null;
        this.transientOffset = null;
        this.transientAmount = null;
    }
    
    // Pageable reflection "magic"
    
    /**
     * Obtains the current view offset using the Java reflection API to
     * get the {@code currentPage} private field from the pageable view and
     * multiplying it by the rows per page. 
     */
    protected long getPageableViewOffset()
    {
        assertPageableView();
        long page = (Long)ReflectUtils.readField(
            this.pageableView, "currentPage"
        );
        return page * getPageableRowsPerPage();
    }
    
    /**
     * Obtains the maximum rows per page needed by the pageable view by
     * calling the
     * {@link AbstractPageableView#getItemsPerPage getItemsPerPage()}
     * method.
     */
    protected long getPageableRowsPerPage()
    {
        assertPageableView();
        return this.pageableView.getItemsPerPage();
    }
    
    /**
     * Returns {@code true} if the desired {@code offset} and {@code amount}
     * are different than the previously cached values.
     */
    private boolean isCacheStale(int offset, int amount)
    {
        boolean stale = false;
        
        if(null == this.transientOffset || null == this.transientAmount)
        {
            stale = true;
        }
        else if(this.transientOffset != offset || this.transientAmount < amount)
        {
            stale = true;
        }
        return stale;
    }
    
    /**
     * Asserts that {@code pageableView} is not {@code null}.
     */
    private void assertPageableView()
    {
        Checks.notNull(
            this.pageableView,
            "setPageableView() must be called before provider can load"
        );
    }
}
