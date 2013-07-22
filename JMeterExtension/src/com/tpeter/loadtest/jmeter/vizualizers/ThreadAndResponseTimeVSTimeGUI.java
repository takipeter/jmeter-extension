package com.tpeter.loadtest.jmeter.vizualizers;

import java.awt.Color;
import java.util.Iterator;

import kg.apc.charting.AbstractGraphRow;
import kg.apc.charting.DateTimeRenderer;
import kg.apc.charting.rows.GraphRowAverages;
import kg.apc.jmeter.graphs.AbstractVsThreadVisualizer;
import kg.apc.jmeter.vizualizers.CompositeResultCollector;
import kg.apc.jmeter.vizualizers.JSettingsPanel;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;

/**
 * Custom graph component for JMeter to display
 * thread count and response time vs. overall time in the same graph
 * 
 * It supports loading from file and provides option for scaling thread count
 * 
 * @author Peter
 *
 */
public class ThreadAndResponseTimeVSTimeGUI extends AbstractVsThreadVisualizer {

	private static final String RESPONSE_TIMES = "Response Times";
	private static final String GRAPH_LABEL = "TPeter - Threads and Response Times vs Times";
	private static final String X_AXIS_LABEL = "Elapsed time";
	private static final String Y_AXIS_LABEL = "Response times in ms and thread count";
	private static final String OVERALL_PREFIX = "Overall ";
	private static final String ACTIVE_THREAD_COUNT_PREFIX = "Active Thread Count (x ";
	private static final String ACTIVE_THREAD_COUNT_POSTFIX = ")";
	
	protected long relativeStartTime = 0;
	private boolean isJtlLoad = false;
	private int thrScaleBy;
	private MySettingsJPanel settingsPanel;

	public ThreadAndResponseTimeVSTimeGUI() {
		super();

		DateTimeRenderer dateTimeRenderer = new DateTimeRenderer(
				DateTimeRenderer.HHMMSS);
		graphPanel.getGraphObject().setxAxisLabelRenderer(dateTimeRenderer);
		graphPanel.getGraphObject().setxAxisLabel(X_AXIS_LABEL);
		graphPanel.getGraphObject().setDisplayPrecision(true);

		// change Y axis for multiple graph names
		graphPanel.getGraphObject().setYAxisLabel(Y_AXIS_LABEL);
	}

	@Override
	public String getLabelResource() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getStaticLabel() {
		return GRAPH_LABEL;
	}

	@Override
	public void add(SampleResult res) {
		if (!isSampleIncluded(res)) {
			return;
		}
		super.add(res);

		if (relativeStartTime == 0) {
			relativeStartTime = JMeterContextService.getTestStartTime();
			isJtlLoad = false;
			if (relativeStartTime == 0) {
				relativeStartTime = res.getStartTime();
				isJtlLoad = true;
			}
			relativeStartTime = relativeStartTime - relativeStartTime
					% getGranulation();
			handleRelativeStartTime();
		}
		if (isJtlLoad) {
			if (relativeStartTime > res.getStartTime()) {
				relativeStartTime = res.getStartTime() - res.getStartTime()
						% getGranulation();
				handleRelativeStartTime();
			}
		}
		
		thrScaleBy = settingsPanel.getThrScaleBy();

		String label = RESPONSE_TIMES;
		String aggLabel = OVERALL_PREFIX + label;
		String thrLabel = ACTIVE_THREAD_COUNT_PREFIX + thrScaleBy + ACTIVE_THREAD_COUNT_POSTFIX;
		String thrAggLabel = OVERALL_PREFIX + thrLabel;

		GraphRowAverages row = (GraphRowAverages) model.get(label);
		GraphRowAverages rowAgg = (GraphRowAverages) modelAggregate
				.get(aggLabel);
		GraphRowAverages rowThr = (GraphRowAverages) model.get(thrLabel);
		GraphRowAverages rowThrAgg = (GraphRowAverages) model.get(thrAggLabel);

		if (row == null || rowThr == null) {
			row = (GraphRowAverages) getNewRow(model,
					AbstractGraphRow.ROW_AVERAGES, label,
					AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false,
					true, false);
			rowThr = (GraphRowAverages) getNewRow(model,
					AbstractGraphRow.ROW_AVERAGES, thrLabel,
					AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false,
					true, Color.GREEN, false);
		}

		if (rowAgg == null || rowThrAgg == null) {
			rowAgg = (GraphRowAverages) getNewRow(modelAggregate,
					AbstractGraphRow.ROW_AVERAGES, aggLabel,
					AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false,
					true, Color.RED, false);
			rowThrAgg = (GraphRowAverages) getNewRow(modelAggregate,
					AbstractGraphRow.ROW_AVERAGES, thrAggLabel,
					AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false,
					true, Color.GREEN, false);
		}

		int threadsCount = getCurrentThreadCount(res);
		rowThr.add(normalizeTime(res.getEndTime()), threadsCount
				* thrScaleBy);
		rowThrAgg.add(normalizeTime(res.getEndTime()), threadsCount
				* thrScaleBy);

		row.add(normalizeTime(res.getEndTime()), res.getTime());
		rowAgg.add(normalizeTime(res.getEndTime()), res.getTime());

		updateGui(null);
	}

	protected void handleRelativeStartTime() {
		if (graphPanel.getGraphObject().getChartSettings().isUseRelativeTime()) {
			graphPanel.getGraphObject().setxAxisLabelRenderer(
					new DateTimeRenderer(DateTimeRenderer.HHMMSS,
							relativeStartTime));
		}
		graphPanel.getGraphObject().setTestStartTime(relativeStartTime);
		graphPanel.getGraphObject().setForcedMinX(relativeStartTime);
	}

	private long normalizeTime(long time) {
		return time - time % getGranulation();
	}

    @Override
    protected JSettingsPanel createSettingsPanel() {
    	settingsPanel =  new MySettingsJPanel(this,
                JSettingsPanel.GRADIENT_OPTION
                | JSettingsPanel.LIMIT_POINT_OPTION
                | JSettingsPanel.HIDE_NON_REP_VALUES_OPTION
                | JSettingsPanel.MAXY_OPTION
                | JSettingsPanel.AGGREGATE_OPTION
                | JSettingsPanel.MARKERS_OPTION);
        
        return settingsPanel;
    }
	
	@Override
	public void clearData() {
		super.clearData();
		
		clearRowsFromCompositeModels(getModel().getName());
		model.clear();
		modelAggregate.clear();
		colors.reset();
		graphPanel.clearRowsTab();

		updateGui();
		repaint();
	}
	
	public void updateData() {
		// clear existing data
		super.clearData();
		
		// rebuild values
		collector = (ResultCollector) createTestElement();
		collector.loadExistingFile();

		// update ui
		updateGui();
	}
	  
	private void clearRowsFromCompositeModels(String vizualizerName) {
		GuiPackage gui = GuiPackage.getInstance();
		JMeterTreeModel testTree = gui.getTreeModel();
		Iterator it = testTree.getNodesOfType(CompositeResultCollector.class)
				.iterator();

		while (it.hasNext()) {
			Object obj = it.next();
			CompositeResultCollector compositeResultCollector = (CompositeResultCollector) ((JMeterTreeNode) obj)
					.getTestElement();

			compositeResultCollector.getCompositeModel().clearRows(
					vizualizerName);
		}
	}

    @Override
    public String getWikiPage() {
        return "ThreadAndResponseTimeVSTime";
    }
	
}
