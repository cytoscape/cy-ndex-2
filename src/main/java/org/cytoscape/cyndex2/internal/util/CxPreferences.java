package org.cytoscape.cyndex2.internal.util;

import java.util.Properties;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.property.CyProperty;

public class CxPreferences {
	public static final String VIEW_THRESHOLD = "viewThreshold";
	private static final int DEF_VIEW_THRESHOLD = 200000;

	public static int getViewThreshold() {
		return getIntegerProperty(VIEW_THRESHOLD, DEF_VIEW_THRESHOLD);
	}

	public static void setViewThreshold(int viewThreshold) {
		setProperty(VIEW_THRESHOLD,  Integer.toString(viewThreshold));
	}
	
	public static final String CREATE_VIEW_PROPERTY = "cx.createView";

	public enum CreateViewEnum {
		ALWAYS("Always", "Always create a view, regardless of network size"),
		AUTO("Auto", "Create views for smaller networks only"),
		NEVER("Never", "Never create views, regardless of network size");

		private final String displayName;
		private final String description;

		private CreateViewEnum(final String displayName, final String description) {
			this.displayName = displayName;
			this.description = description;
		}

		public String toString() {
			return displayName;
		}

		public String getDescription() {
			return description;
		}
	}

	public static CreateViewEnum getCreateView() {
		final String property = getProperty(CREATE_VIEW_PROPERTY);
		return CreateViewEnum.ALWAYS.toString().toLowerCase().equals(property) ? CreateViewEnum.ALWAYS
				: CreateViewEnum.NEVER.toString().toLowerCase().equals(property) ? CreateViewEnum.NEVER
						: CreateViewEnum.AUTO;
	}

	public static void setCreateView(CreateViewEnum createView) {
		setProperty(CREATE_VIEW_PROPERTY, createView.toString().toLowerCase());
	}
	
	public static final String APPLY_LAYOUT_PROPERTY = "cx.applyLayout";

	public enum ApplyLayoutEnum {
		AUTO("Auto", "Apply a layout for smaller networks only"),
		NEVER("Never", "Never apply a layout, regardless of network size");

		private final String displayName;
		private final String description;

		private ApplyLayoutEnum(final String displayName, final String description) {
			this.displayName = displayName;
			this.description = description;
		}

		public String toString() {
			return displayName;
		}

		public String getDescription() {
			return description;
		}
	}

	public static ApplyLayoutEnum getApplyLayout() {
		final String property = getProperty(APPLY_LAYOUT_PROPERTY);
		return ApplyLayoutEnum.NEVER.toString().toLowerCase().equals(property) ? ApplyLayoutEnum.NEVER
				: ApplyLayoutEnum.AUTO;
	}

	public static void setApplyLayout(ApplyLayoutEnum applyLayout) {
		setProperty(APPLY_LAYOUT_PROPERTY, applyLayout.toString().toLowerCase());
	}
	
	public static final String LARGE_LAYOUT_THRESHOLD_PROPERTY = "cx.largeLayoutThreshold";

	public static final int DEF_LARGE_LAYOUT_THRESHOLD = 25000;

	public static Integer getLargeLayoutThreshold() {
		return getIntegerProperty(LARGE_LAYOUT_THRESHOLD_PROPERTY, DEF_LARGE_LAYOUT_THRESHOLD);
	}

	public static void setLargeLayoutThreshold(int largeLayoutThreshold) {
		setProperty(LARGE_LAYOUT_THRESHOLD_PROPERTY,  Integer.toString(largeLayoutThreshold));
	}
	
	private static String getProperty(String key) {
		final Properties props = (Properties) CyServiceModule
				.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)").getProperties();
		return props.getProperty(key);
	}

	private static Object setProperty(String key, String property) {
		final Properties props = (Properties) CyServiceModule
				.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)").getProperties();
		return props.setProperty(key, property);
	}

	private static Integer getIntegerProperty(String key, Integer defaultValue) {
		final String property = getProperty(key);

		try {
			return Integer.parseInt(property);
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
