package org.systemsbiology.chem.app;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import com.jrefinery.chart.*;
import com.jrefinery.chart.axis.*;
import com.jrefinery.chart.plot.*;
import com.jrefinery.chart.tooltips.*;
import com.jrefinery.chart.urls.*;
import com.jrefinery.chart.renderer.*;
import com.jrefinery.data.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.text.*;
import javax.imageio.*;
import java.awt.datatransfer.*;
import org.systemsbiology.util.*;
import org.systemsbiology.chem.*;
import org.systemsbiology.gui.*;

/**
 * Displays a plot of simulation results.
 */
public class SimulationResultsPlot extends JFrame
{
    private static final int DEFAULT_PLOT_WIDTH_PIXELS = 500;
    private static final int DEFAULT_PLOT_HEIGHT_PIXELS = 450;
    private static final int MIN_PLOT_WIDTH_PIXELS = 400;
    private static final int MIN_PLOT_HEIGHT_PIXELS = 400;
    private static final int SCROLL_PANE_OFFSET_PIXELS = 20;
    private static final String OUTPUT_IMAGE_TYPE_EXTENSION = "png";

    public static final int MAX_NUM_SYMBOLS_TO_PLOT = 20;

    private static File sSaveDirectory;
    private static boolean sSystemClipboardSupportsImageTransfer;

    static
    {
        sSystemClipboardSupportsImageTransfer = ImageTransferHandler.checkDoesSystemClipboardSupportImageTransfer();
        sSaveDirectory = null;
    }

    private String mLabel;
    private int mOrigFrameHeightPixels;
    private int mOrigFrameWidthPixels;
    private int mPlotHeightPixels;
    private int mPlotWidthPixels;
    private JLabel mImageLabel;
    private JFreeChart mChart;
    private JScrollPane mPlotScrollPane;
    private BufferedImage mPlotImage;
    private SimulationResults mSimulationResults;

    public SimulationResultsPlot(SimulationResults pSimulationResults, String pAppName, String pLabel) throws IOException
    {
        super(pAppName + ": results");
        mLabel = pLabel;
        mSimulationResults = pSimulationResults;
        
        initialize();
    }


    private void initialize() throws IOException
    {
        mPlotHeightPixels = DEFAULT_PLOT_HEIGHT_PIXELS;
        mPlotWidthPixels = DEFAULT_PLOT_WIDTH_PIXELS;

        JPanel bigPanel = new JPanel();
        
        JPanel labelPanel = new JPanel();
        JLabel plotLabel = new JLabel(mLabel);
        labelPanel.add(plotLabel);
        
        Box plotBox = new Box(BoxLayout.Y_AXIS);
        JLabel imageLabel = new JLabel();
        imageLabel.setTransferHandler(new ImageTransferHandler());

            
        mImageLabel = imageLabel;

        mChart = generateChart(mSimulationResults);
        updatePlotImage();

        JButton saveButton = new JButton("save as " + OUTPUT_IMAGE_TYPE_EXTENSION.toUpperCase() + " image file");
        saveButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleSaveButton();
            }
        });

        plotBox.add(labelPanel);

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(DEFAULT_PLOT_WIDTH_PIXELS + SCROLL_PANE_OFFSET_PIXELS, DEFAULT_PLOT_HEIGHT_PIXELS + SCROLL_PANE_OFFSET_PIXELS));
        mPlotScrollPane = scrollPane;
        
        plotBox.add(scrollPane);
        
        JPanel buttonPanel = new JPanel();
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add(saveButton);

        if(sSystemClipboardSupportsImageTransfer)
        {
            JButton copyButton = new JButton("copy to clipboard");
            copyButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    try
                    {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        TransferHandler handler = mImageLabel.getTransferHandler();
                        handler.exportToClipboard(mImageLabel, clipboard, TransferHandler.COPY);
                    }
                    catch(Exception e)
                    {
                        ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
                        optionPane.createDialog(SimulationResultsPlot.this,
                                                "failed to copy plot image").show();
                    }
                }
            });
            buttonBox.add(copyButton);
        }
        
        buttonPanel.add(buttonBox);

        plotBox.add(buttonPanel);

        bigPanel.add(plotBox);
        setContentPane(bigPanel);

        pack();

        mOrigFrameHeightPixels = getHeight();
        mOrigFrameWidthPixels = getWidth();

        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e) 
            {
                handleResize();
                show();
            }
        });

    }

    public void handleResize()
    {
        int newFrameWidthPixels = getWidth();
        int newFrameHeightPixels = getHeight();
            
        int origFrameWidthPixels = mOrigFrameWidthPixels;
        int origFrameHeightPixels = mOrigFrameHeightPixels;
            
        int changeFrameWidthPixels = newFrameWidthPixels - origFrameWidthPixels;
        int changeFrameHeightPixels = newFrameHeightPixels - origFrameHeightPixels;

        int newPlotWidthPixels = DEFAULT_PLOT_WIDTH_PIXELS + changeFrameWidthPixels;
        int newPlotHeightPixels = DEFAULT_PLOT_HEIGHT_PIXELS + changeFrameHeightPixels;
        mPlotScrollPane.setPreferredSize(new Dimension(newPlotWidthPixels + SCROLL_PANE_OFFSET_PIXELS,
                                                       newPlotHeightPixels + SCROLL_PANE_OFFSET_PIXELS));

        if(newPlotWidthPixels < MIN_PLOT_WIDTH_PIXELS)
        {
            newPlotWidthPixels = MIN_PLOT_WIDTH_PIXELS;
        }
        if(newPlotHeightPixels < MIN_PLOT_HEIGHT_PIXELS)
        {
            newPlotHeightPixels = MIN_PLOT_HEIGHT_PIXELS;
        }
        mPlotWidthPixels = newPlotWidthPixels;
        mPlotHeightPixels = newPlotHeightPixels;

        try
        {
            updatePlotImage();
        }
        catch(IOException e2)
        {
            throw new RuntimeException(e2);
        }

        mImageLabel.setPreferredSize(new Dimension(mPlotWidthPixels, mPlotHeightPixels));
        mImageLabel.revalidate();
        mImageLabel.repaint();
    }
             
    private void handleSaveButton()
    {
        FileChooser fileChooser = new FileChooser(this);
        fileChooser.setDialogTitle("please choose the file for saving the image:");
        fileChooser.setApproveButtonText("save");
        fileChooser.show();
        if(null != sSaveDirectory)
        {
            fileChooser.setCurrentDirectory(sSaveDirectory);
        }
        File selectedFile = fileChooser.getSelectedFile();
        if(null != selectedFile)
        {
            boolean doSave = true;
            String selectedFileName = selectedFile.getAbsolutePath();
            if(selectedFile.exists())
            {
                doSave = FileChooser.handleOutputFileAlreadyExists(this, selectedFileName);
            }
            if(doSave)
            {
                File parentDirectory = selectedFile.getParentFile();
                sSaveDirectory = parentDirectory;
                if(! selectedFileName.matches(".*\\." + OUTPUT_IMAGE_TYPE_EXTENSION + "$"))
                {
                    if(-1 != selectedFileName.indexOf('.'))
                    {
                        SimpleTextArea textArea = new SimpleTextArea("The output file you selected has a non-standard name for this image type:\n" + selectedFileName + "\nAre you sure that you wish to save the image using this name?");
                        JOptionPane messageDialog = new JOptionPane(textArea);
                        messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                        messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                        messageDialog.createDialog(this, "Use non-standard file name?").show();
                        Integer response = (Integer) messageDialog.getValue();
                        if(null != response &&
                           response.intValue() == JOptionPane.YES_OPTION)
                        {
                            // do nothing
                        }
                        else
                        {
                            doSave = false;
                        }
                    }
                    else
                    {
                        selectedFileName = selectedFileName + "." + OUTPUT_IMAGE_TYPE_EXTENSION;
                    }
                }
                try
                {
                    ImageIO.write(mPlotImage, OUTPUT_IMAGE_TYPE_EXTENSION, selectedFile);
                }
                catch(Exception e)
                {
                    ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e);
                    optionPane.createDialog(this, 
                                            "failed to save plot image").show();
                }
            }
        }
    }

    void updatePlotImage() throws IOException
    {
        BufferedImage plotImage = mChart.createBufferedImage(mPlotWidthPixels, mPlotHeightPixels);
        mPlotImage = plotImage;
        mImageLabel.setIcon(new ImageIcon(plotImage));
    }

    protected static JFreeChart generateChart(SimulationResults pSimulationResults)
    {
        String []resultsSymbolNames = pSimulationResults.getResultsSymbolNames();
        double []resultsTimeValues = pSimulationResults.getResultsTimeValues();
        Object []resultsSymbolValues = pSimulationResults.getResultsSymbolValues();

        int numSymbols = resultsSymbolNames.length;
        int numTimePoints = resultsTimeValues.length;

        ArrayList seriesVec = new ArrayList();
        double []timeSnapshot = null;
        for(int i = 0; i < numSymbols; ++i)
        {
            String symbolName = resultsSymbolNames[i];
            XYSeries series = new XYSeries(symbolName);
            for(int j = 0; j < numTimePoints; ++j)
            {
                timeSnapshot = (double []) resultsSymbolValues[j];
                series.add(resultsTimeValues[j], timeSnapshot[i]);
            }
            
            seriesVec.add(series);
        }
        XYSeriesCollection seriesColl = new XYSeriesCollection();
        Iterator seriesIter = seriesVec.iterator();
        while(seriesIter.hasNext())
        {
            XYSeries series = (XYSeries) seriesIter.next();
            seriesColl.addSeries(series);
        }

        NumberAxis xAxis = new HorizontalNumberAxis("time");
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new VerticalNumberAxis("value");
        XYPlot plot = new XYPlot(seriesColl, xAxis, yAxis);
        XYToolTipGenerator toolTipGen = null;
        XYURLGenerator urlGen = null;
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES, toolTipGen, urlGen);
        plot.setRenderer(renderer);
        JFreeChart chart = new JFreeChart(null, null, plot, true);

        return(chart);
    }
}
