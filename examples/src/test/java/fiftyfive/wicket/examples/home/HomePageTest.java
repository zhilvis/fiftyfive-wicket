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
package fiftyfive.wicket.examples.home;

import fiftyfive.wicket.examples.BaseWicketUnitTest;
import fiftyfive.wicket.test.XHtmlValidator;
import org.junit.Test;
import org.apache.wicket.spring.test.ApplicationContextMock;

public class HomePageTest extends BaseWicketUnitTest
{
    protected void initSpringContext(ApplicationContextMock ctx)
    {
        // If HomePage had @SpringBean dependencies, you would mock them here.
        // Like this:
        // MockitoAnnotations.initMocks(this);
        // ctx.putBean(_mockSvc);
    }
    
    @Test
    public void testRender() throws Exception
    {
        _tester.startPage(HomePage.class);
        _tester.assertRenderedPage(HomePage.class);
        XHtmlValidator.assertValidMarkup(_tester);
    }
}