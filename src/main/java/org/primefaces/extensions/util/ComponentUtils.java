/*
 * Copyright 2011 PrimeFaces Extensions.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */

package org.primefaces.extensions.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.render.Renderer;

import org.primefaces.component.api.AjaxSource;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.component.commandlink.CommandLink;
import org.primefaces.component.hotkey.Hotkey;
import org.primefaces.component.menuitem.MenuItem;
import org.primefaces.component.splitbutton.SplitButton;
import org.primefaces.extensions.component.base.Attachable;
import org.primefaces.extensions.component.base.EnhancedAttachable;

/**
 * Component utils for this project.
 *
 * @author  Oleg Varaksin / last modified by $Author$
 * @version $Revision$
 * @since   0.2
 */
public class ComponentUtils extends org.primefaces.util.ComponentUtils {

	private static final Logger LOG = Logger.getLogger(ComponentUtils.class.getName());

	public static String escapeComponentId(final String id) {
		return id.replaceAll(":", "\\\\\\\\:");
	}

	public static List<UIComponent> findComponents(FacesContext context, UIComponent source, String list) {
		final List<UIComponent> foundComponents = new ArrayList<UIComponent>();

		final String[] ids = list.split("[\\s,]+");

		for (int i = 0; i < ids.length; i++) {
			final String id = ids[i];

			if (id.equals("@this")) {
				foundComponents.add(source);
			} else if (id.equals("@form")) {
				final UIComponent form = ComponentUtils.findParentForm(context, source);

				if (form != null) {
					foundComponents.add(form);
				} else if (context.isProjectStage(ProjectStage.Development)) {
					LOG.log(Level.INFO, "Cannot find enclosing form for component \"{0}\".", source.getClientId(context));
				}
			} else if (id.equals("@parent")) {
				foundComponents.add(source.getParent());
			} else if (id.equals("@all") || id.equals("@none")) {
				LOG.log(Level.WARNING, "Components @all and @none are not supported.");
			} else {
				final UIComponent component = source.findComponent(id);

				if (component != null) {
					foundComponents.add(component);
				} else if (context.isProjectStage(ProjectStage.Development)) {
					LOG.log(Level.WARNING, "Cannot find component with identifier \"{0}\" in view.", id);
				}
			}
		}

		return foundComponents;
	}

	public static String findTarget(FacesContext context, Attachable attachable) {
		if (!(attachable instanceof UIComponent)) {
			throw new FacesException("An attachable component must extend UIComponent or ClientBehavior.");
		}

		return findTarget(context, attachable, (UIComponent) attachable);
	}

	public static String findTarget(FacesContext context, Attachable attachable, ClientBehaviorContext cbContext) {
		if (!(attachable instanceof ClientBehavior)) {
			throw new FacesException("An attachable component must extend UIComponent or ClientBehavior.");
		}

		if (cbContext == null) {
			throw new FacesException("ClientBehaviorContext is null.");
		}

		return findTarget(context, attachable, cbContext.getComponent());
	}

	private static String findTarget(FacesContext context, Attachable attachable, UIComponent component) {
		final String forValue = attachable.getFor();
		if (forValue != null) {
			final UIComponent forComponent = component.findComponent(forValue);
			if (forComponent == null) {
				throw new FacesException("Cannot find component '" + forValue + "'.");
			}

			return escapeJQueryId(forComponent.getClientId(context));
		}

		if (attachable instanceof EnhancedAttachable) {
			final EnhancedAttachable enhancedAttachable = (EnhancedAttachable) attachable;
			final String forSelector = enhancedAttachable.getForSelector();

			if (forSelector != null) {
				if (forSelector.startsWith("#")) {
					return escapeComponentId(forSelector);
				} else {
					return escapeText(forSelector);
				}
			}
		}

		return escapeJQueryId(component.getParent().getClientId(context));
	}

	public static void addComponentResource(FacesContext context, String name) {
		addComponentResource(context, name, Constants.LIBRARY, "head");
	}

	public static void addComponentResource(FacesContext context, String name, String library, String target) {
		final Application application = context.getApplication();

		final UIComponent componentResource = application.createComponent(UIOutput.COMPONENT_TYPE);
		componentResource.setRendererType(application.getResourceHandler().getRendererTypeForResourceName(name));
		componentResource.setTransient(true);
		componentResource.setId(context.getViewRoot().createUniqueId());
		componentResource.getAttributes().put("name", name);
		componentResource.getAttributes().put("library", library);
		componentResource.getAttributes().put("target", target);

		context.getViewRoot().addComponentResource(context, componentResource, target);
	}

	/**
	 * Duplicate code from json-simple project under apache license
	 * http://code.google.com/p/json-simple/source/browse/trunk/src/org/json/simple/JSONValue.java
	 *
	 * @param  text original text as string
	 * @return String escaped text as string to be used as JSON value
	 */
	public static String escapeText(final String text) {
		if (text == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;

			case '\\':
				sb.append("\\\\");
				break;

			case '\b':
				sb.append("\\b");
				break;

			case '\f':
				sb.append("\\f");
				break;

			case '\n':
				sb.append("\\n");
				break;

			case '\r':
				sb.append("\\r");
				break;

			case '\t':
				sb.append("\\t");
				break;

			case '/':
				sb.append("\\/");
				break;

			default:

				//Reference: http://www.unicode.org/versions/Unicode5.1.0/
				if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F')
				    || (ch >= '\u2000' && ch <= '\u20FF')) {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}

					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}

		return sb.toString();
	}

	public static Object getConvertedSubmittedValue(FacesContext fc, EditableValueHolder evh) {
		Object submittedValue = evh.getSubmittedValue();
		if (submittedValue == null) {
			return submittedValue;
		}

		try {
			UIComponent component = (UIComponent) evh;
			Renderer renderer = getRenderer(fc, component);
			if (renderer != null) {
				// convert submitted value by renderer
				return renderer.getConvertedValue(fc, component, submittedValue);
			} else if (submittedValue instanceof String) {
				// convert submitted value by registred (implicit or explicit) converter
				Converter converter = getConverter(fc, component);
				if (converter != null) {
					return converter.getAsObject(fc, component, (String) submittedValue);
				}
			}
		} catch (Exception e) {
			// an conversion error occured
		}

		return submittedValue;
	}

	public static Renderer getRenderer(FacesContext fc, UIComponent component) {
		String rendererType = component.getRendererType();
		if (rendererType != null) {
			return fc.getRenderKit().getRenderer(component.getFamily(), rendererType);
		}

		return null;
	}

	public static Converter getConverter(FacesContext fc, UIComponent component) {
		if (!(component instanceof EditableValueHolder)) {
			return null;
		}

		Converter converter = ((EditableValueHolder) component).getConverter();
		if (converter != null) {
			return converter;
		}

		ValueExpression valueExpression = component.getValueExpression("value");
		if (valueExpression == null) {
			return null;
		}

		Class<?> converterType = valueExpression.getType(fc.getELContext());
		if (converterType == null || converterType == String.class || converterType == Object.class) {
			// no conversion is needed
			return null;
		}

		return fc.getApplication().createConverter(converterType);
	}

	public static boolean isAjaxifiedComponent(UIComponent component) {
		// check for ajax source
		if (component instanceof AjaxSource) {
			// workaround, currently there isn't other way in PrimeFaces
			boolean isAjaxified;

			if (component instanceof CommandButton) {
				String type = ((CommandButton) component).getType();
				isAjaxified = !type.equals("reset") && !type.equals("button") && ((CommandButton) component).isAjax();
			} else if (component instanceof CommandLink) {
				isAjaxified = ((CommandLink) component).isAjax();
			} else if (component instanceof MenuItem) {
				isAjaxified = ((MenuItem) component).getUrl() == null && ((MenuItem) component).isAjax();
			} else if (component instanceof SplitButton) {
				isAjaxified = ((SplitButton) component).isAjax();
			} else if (component instanceof Hotkey) {
				isAjaxified = ((Hotkey) component).getHandler() == null;
			} else {
				isAjaxified = true;
			}

			if (isAjaxified) {
				return true;
			}
		}

		if (component instanceof ClientBehaviorHolder) {
			// check for attached f:ajax / p:ajax
			Collection<List<ClientBehavior>> behaviors = ((ClientBehaviorHolder) component).getClientBehaviors().values();
			if (behaviors != null && !behaviors.isEmpty()) {
				for (List<ClientBehavior> listBehaviors : behaviors) {
					for (ClientBehavior clientBehavior : listBehaviors) {
						if (clientBehavior instanceof javax.faces.component.behavior.AjaxBehavior
						    || clientBehavior instanceof org.primefaces.component.behavior.ajax.AjaxBehavior) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}
}