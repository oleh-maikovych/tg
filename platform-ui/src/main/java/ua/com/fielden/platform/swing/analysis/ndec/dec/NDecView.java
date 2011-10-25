package ua.com.fielden.platform.swing.analysis.ndec.dec;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import ua.com.fielden.platform.swing.analysis.ndec.DecChartPanel;
import ua.com.fielden.platform.swing.analysis.ndec.IAnalysisDoubleClickListener;
import ua.com.fielden.platform.swing.view.BasePanel;

public class NDecView extends BasePanel{

    private static final long serialVersionUID = -4573644376854599911L;

    private static final int DEFAULT_MIN_HEIGHT = 100;
    private static final int DEFAULT_MIN_WIDTH = 200;
    //private final NDecModel model;

    private final JComponent[][] components;

    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int minWidth = DEFAULT_MIN_WIDTH;

    public NDecView(final NDecModel model, final int minHeight){
	this(model);
	this.minHeight = minHeight;
    }

    public NDecView(final NDecModel model){
	super(new MigLayout("fill, insets 0"));
	//this.model = model;
	components = new JComponent[model.getDecCount()*2][];
	final JPanel decPanel = new JPanel(new MigLayout("fill, insets 0","[l][c]","[fill, grow]"));
	for(int decIndex = 0; decIndex < model.getDecCount(); decIndex++){
	    final DecView decView = new DecView(model.getDec(decIndex));
	    final JLabel chartTitle = getChartTitle(model.getDec(decIndex));
	    final JLabel stubLabel = stubLablel();
	    final JPanel calculatedNumberPanel = decView.getCalculatedNumberPanel();
	    final DecChartPanel chartPanel = decView.getChartPanel();

	    components[2 * decIndex] = new JComponent[2];
	    components[2 * decIndex + 1] = new JComponent[2];
	    components[2 * decIndex][0] = stubLabel;
	    components[2 * decIndex][1] = chartTitle;
	    components[2 * decIndex + 1][0] = calculatedNumberPanel;
	    components[2 * decIndex + 1][1] = chartPanel;

	    decPanel.add(stubLabel);
	    decPanel.add(chartTitle, "wrap");
	    decPanel.add(decView.getCalculatedNumberPanel());
	    decPanel.add(decView.getChartPanel(), "wrap, growx, push");
	}
	final JScrollPane scrollPane = new JScrollPane(decPanel);
	//scrollPane.addComponentListener(createComponentResizedAdapter(scrollPane));
	add(scrollPane, "grow");
    }

    private ComponentListener createComponentResizedAdapter(final JScrollPane scrollPane){
	return new ComponentAdapter() {
	    @Override
	    public void componentResized(final ComponentEvent e) {
		changeComponentSize(scrollPane);
	    }
	};
    }

    private void changeComponentSize(final JScrollPane scrollPane){
	int height = scrollPane.getHeight();
	int width = scrollPane.getWidth();

	for(int componentIndex = 0; componentIndex < (components.length / 2); componentIndex++){
	    height -= components[componentIndex * 2][0].getHeight();
	}
	width -= components.length >=1  ? components[1][0].getWidth() : 0;

	height = height / (components.length / 2);
	if(height < minHeight){
	    height = minHeight;
	}
	if(width < minWidth){
	    width = minWidth;
	}
	for(int componentIndex = 0; componentIndex < (components.length / 2); componentIndex++){
	    final int titleHeight = components[componentIndex * 2][0].getPreferredSize().height;
	    final int titleWidth = components[componentIndex * 2 + 1][0].getWidth();
	    components[componentIndex * 2][0].setPreferredSize(new Dimension(titleWidth, titleHeight));
	    components[componentIndex * 2][1].setPreferredSize(new Dimension(width, titleHeight));
	    components[componentIndex * 2 + 1][0].setPreferredSize(new Dimension(titleWidth, height));
	    components[componentIndex * 2 + 1][1].setPreferredSize(new Dimension(width, height));
	}
    }

    public void setMinChartHeight(final int minHeight){
	this.minHeight = minHeight;
    }

    public void setMinChartWidth(final int minWidth){
	this.minWidth = minWidth;
    }


    private JLabel stubLablel(){
	return new JLabel("");
    }

    private JLabel getChartTitle(final DecModel decModel){
	final JLabel label = new JLabel(decModel.getChartName());
	label.setFont(new Font("SansSerif", Font.BOLD, 18));
	label.setHorizontalTextPosition(JLabel.CENTER);
	return label;
    }


    @Override
    public String getInfo() {
	return "Multiple dec view";
    }

    public void addAnalysisDoubleClickListener(final IAnalysisDoubleClickListener l){
	for(int componentIndex = 0; componentIndex < (components.length / 2); componentIndex++){
	    ((DecChartPanel)components[componentIndex * 2 + 1][1]).addAnalysisDoubleClickListener(l);
	}
    }

    public void removeAnalysisDoubleClickListener(final IAnalysisDoubleClickListener l){
	for(int componentIndex = 0; componentIndex < (components.length / 2); componentIndex++){
	    ((DecChartPanel)components[componentIndex * 2 + 1][1]).removeAnalysisDoubleClickListener(l);
	}
    }

}
