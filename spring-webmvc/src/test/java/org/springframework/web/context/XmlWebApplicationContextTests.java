/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.testfixture.AbstractApplicationContextTests;
import org.springframework.context.testfixture.beans.TestApplicationListener;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.ComplexWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.SimpleWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class XmlWebApplicationContextTests extends AbstractApplicationContextTests {


	private static final String URL_KNOWN_ONLY_PARENT = "/knownOnlyToParent.do";

	private MockServletConfig servletConfig;

	private ConfigurableWebApplicationContext root;

	private XmlWebApplicationContext wac;


	private ServletContext getServletContext() {
		return servletConfig.getServletContext();
	}


	@Override
	protected ConfigurableApplicationContext createContext() throws Exception {
		InitAndIB.constructed = false;
		// 创建 Root 容器
		root = new XmlWebApplicationContext();
		root.getEnvironment().addActiveProfile("rootProfile1");
		// 创建一个 ServletContext
		MockServletContext sc = new MockServletContext("");
		servletConfig = new MockServletConfig(sc, "simple");

		root.setServletContext(sc);
		// 设置配置文件路径
		root.setConfigLocations("/org/springframework/web/context/WEB-INF/applicationContext.xml");
		// 添加 bean 工厂后置处理器
		root.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
				// 添加后置处理器
				beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
					@Override
					public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
						// 在 bean 初始化之前，对 TestBean 类型的 bean 添加 friends 属性
						if (bean instanceof TestBean) {
							((TestBean) bean).getFriends().add("myFriend");
						}
						return bean;
					}
					@Override
					public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
						return bean;
					}
				});
			}
		});
		// 初始化容器
		root.refresh();
		// 创建一个 web 子容器
		// XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac = new XmlWebApplicationContext();
		// 为子容器设置环境激活属性
		wac.getEnvironment().addActiveProfile("wacProfile1");
		// 为子容器设置父容器
		wac.setParent(root);
		// 设置 ServletContext
		wac.setServletContext(sc);
		// 设命名空间
		wac.setNamespace("test-servlet");
		// 设置配置文件路径
		wac.setConfigLocations("/org/springframework/web/context/WEB-INF/test-servlet.xml");
		wac.refresh();
		return wac;
	}


	@Test
	public void testSpringMvc() throws ServletException, IOException {
		// 创建一个
		DispatcherServlet dispatcherServlet = new DispatcherServlet(wac);
		// 设置 web 容器
		dispatcherServlet.init(servletConfig);

		MockHttpServletRequest request = new MockHttpServletRequest(getServletContext(), "GET", "/controller/hello?name=123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);

		String contentAsString = response.getContentAsString();
		System.out.println("contentAsString = " + contentAsString);

		dispatcherServlet.destroy();
	}


	@RequestMapping(value = "/controller")
	public static class MyController {

		@GetMapping(value = "/hello")
		public String hello(String name){
			System.out.println("name="+name);
			return "hello "+name+"!";
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void environmentMerge() {
		assertThat(this.root.getEnvironment().acceptsProfiles("rootProfile1")).isTrue();
		assertThat(this.root.getEnvironment().acceptsProfiles("wacProfile1")).isFalse();
		assertThat(this.applicationContext.getEnvironment().acceptsProfiles("rootProfile1")).isTrue();
		assertThat(this.applicationContext.getEnvironment().acceptsProfiles("wacProfile1")).isTrue();
	}

	/**
	 * Overridden as we can't trust superclass method
	 * @see org.springframework.context.testfixture.AbstractApplicationContextTests#testEvents()
	 */
	@Override
	protected void doTestEvents(TestApplicationListener listener, TestApplicationListener parentListener,
			MyEvent event) {
		TestApplicationListener listenerBean = (TestApplicationListener) this.applicationContext.getBean("testListener");
		TestApplicationListener parentListenerBean = (TestApplicationListener) this.applicationContext.getParent().getBean("parentListener");
		super.doTestEvents(listenerBean, parentListenerBean, event);
	}

	@Test
	@Override
	public void count() {
		assertThat(this.applicationContext.getBeanDefinitionCount() == 14).as("should have 14 beans, not "+ this.applicationContext.getBeanDefinitionCount()).isTrue();
	}

	@Test
	@SuppressWarnings("resource")
	public void withoutMessageSource() throws Exception {
		MockServletContext sc = new MockServletContext("");
		XmlWebApplicationContext wac = new XmlWebApplicationContext();
		wac.setParent(root);
		wac.setServletContext(sc);
		wac.setNamespace("testNamespace");
		wac.setConfigLocations("/org/springframework/web/context/WEB-INF/test-servlet.xml");
		wac.refresh();
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				wac.getMessage("someMessage", null, Locale.getDefault()));
		String msg = wac.getMessage("someMessage", null, "default", Locale.getDefault());
		assertThat("default".equals(msg)).as("Default message returned").isTrue();
	}

	@Test
	public void contextNesting() {
		TestBean father = (TestBean) this.applicationContext.getBean("father");
		assertThat(father != null).as("Bean from root context").isTrue();
		assertThat(father.getFriends().contains("myFriend")).as("Custom BeanPostProcessor applied").isTrue();

		TestBean rod = (TestBean) this.applicationContext.getBean("rod");
		assertThat("Rod".equals(rod.getName())).as("Bean from child context").isTrue();
		assertThat(rod.getSpouse() == father).as("Bean has external reference").isTrue();
		assertThat(!rod.getFriends().contains("myFriend")).as("Custom BeanPostProcessor not applied").isTrue();

		rod = (TestBean) this.root.getBean("rod");
		assertThat("Roderick".equals(rod.getName())).as("Bean from root context").isTrue();
		assertThat(rod.getFriends().contains("myFriend")).as("Custom BeanPostProcessor applied").isTrue();
	}

	@Test
	public void initializingBeanAndInitMethod() throws Exception {
		assertThat(InitAndIB.constructed).isFalse();
		InitAndIB iib = (InitAndIB) this.applicationContext.getBean("init-and-ib");
		assertThat(InitAndIB.constructed).isTrue();
		assertThat(iib.afterPropertiesSetInvoked && iib.initMethodInvoked).isTrue();
		assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
		this.applicationContext.close();
		assertThat(!iib.destroyed && !iib.customDestroyed).isTrue();
		ConfigurableApplicationContext parent = (ConfigurableApplicationContext) this.applicationContext.getParent();
		parent.close();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
		parent.close();
		assertThat(iib.destroyed && iib.customDestroyed).isTrue();
	}


	public static class InitAndIB implements InitializingBean, DisposableBean {

		public static boolean constructed;

		public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

		public InitAndIB() {
			constructed = true;
		}

		@Override
		public void afterPropertiesSet() {
			assertThat(this.initMethodInvoked).isFalse();
			this.afterPropertiesSetInvoked = true;
		}

		/** Init method */
		public void customInit() throws ServletException {
			assertThat(this.afterPropertiesSetInvoked).isTrue();
			this.initMethodInvoked = true;
		}

		@Override
		public void destroy() {
			assertThat(this.customDestroyed).isFalse();
			Assert.state(!this.destroyed, "Already destroyed");
			this.destroyed = true;
		}

		public void customDestroy() {
			assertThat(this.destroyed).isTrue();
			Assert.state(!this.customDestroyed, "Already customDestroyed");
			this.customDestroyed = true;
		}
	}

}
