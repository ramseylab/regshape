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

public class Plotter 
{
    private static final int DEFAULT_PLOT_WIDTH_PIXELS = 500;
    private static final int DEFAULT_PLOT_HEIGHT_PIXELS = 500;
    private static final int MIN_PLOT_WIDTH_PIXELS = 400;
    private static final int MIN_PLOT_HEIGHT_PIXELS = 400;
    private static final int SCROLL_PANE_OFFSET_PIXELS = 20;
    private static final String OUTPUT_IMAGE_TYPE_EXTENSION = "png";

    public static final int MAX_NUM_SYMBOLS_TO_PLOT = 20;

    private FramePlacer mFramePlacer;
    private File mSaveDirectory;
    private static boolean sSystemClipboardSupportsImageTransfer;

    static
    {
        sSystemClipboardSupportsImageTransfer = ImageTransferHandler.checkDoesSystemClipboardSupportImageTransfer();
    }

    public Plotter()
    {
        mFramePlacer = new FramePlacer();
        mSaveDirectory = null;
    }

    class Plot extends JFrame
    {
        String mSimulatorAlias;
        String mModelName;
        Date mSimulationDate;
        int mOrigFrameHeightPixels;
        int mOrigFrameWidthPixels;
        int mPlotHeightPixels;
        int mPlotWidthPixels;
        JLabel mPlotLabel;
        JFreeChart mChart;
        JScrollPane mPlotScrollPane;
        BufferedImage mPlotImage;

        public Plot(String pAppName, String pData, String pSimulatorAlias, String pModelName) throws IOException
        {
            super(pAppName + ": results");
            mSimulatorAlias = pSimulatorAlias;
            mModelName = pModelName;
            mSimulationDate = new Date(System.currentTimeMillis());
            mPlotHeightPixels = DEFAULT_PLOT_HEIGHT_PIXELS;
            mPlotWidthPixels = DEFAULT_PLOT_WIDTH_PIXELS;

            JPanel bigPanel = new JPanel();

            Box plotBox = new Box(BoxLayout.Y_AXIS);
            JLabel plotLabel = new JLabel();
            plotLabel.setTransferHandler(new ImageTransferHandler());
            JPanel labelPanel = new JPanel();

            mPlotLabel = plotLabel;

            mChart = generateChart(pData, this);
            updatePlotImage();

            JButton saveButton = new JButton("save as " + OUTPUT_IMAGE_TYPE_EXTENSION.toUpperCase() + " image file");
            saveButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    handleSaveButton();
                }
            });

            JScrollPane scrollPane = new JScrollPane(plotLabel);
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
                            TransferHandler handler = mPlotLabel.getTransferHandler();
                            handler.exportToClipboard(mPlotLabel, clipboard, TransferHandler.COPY);
                        }
                        catch(Exception e)
                        {
                            ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(Plot.this, "failed to copy plot image", e);
                            dialog.show();
                            
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

            mPlotLabel.setPreferredSize(new Dimension(mPlotWidthPixels, mPlotHeightPixels));
            mPlotLabel.revalidate();
        }
             
        private void handleSaveButton()
        {
            FileChooser fileChooser = new FileChooser(this);
            fileChooser.setDialogTitle("please choose the file for saving the imsage:");
            fileChooser.setApproveButtonText("save");
            fileChooser.show();
            if(null != mSaveDirectory)
            {
                fileChooser.setCurrentDirectory(mSaveDirectory);
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
                    mSaveDirectory = parentDirectory;
                    if(! selectedFileName.matches(".*\\." + OUTPUT_IMAGE_TYPE_EXTENSION + "$"))
                    {
                        if(-1 != selectedFileName.indexOf('.'))
                        {
                            SimpleTextArea textArea = new SimpleTextArea("The output file you selected has a non-standard name for this image type:\n" + selectedFileName + "\nAre you sure that you wish to save the image using this name?");
                            SimpleDialog messageDialog = new SimpleDialog(this, 
                                                                          "Use non-standard file name?",
                                                                          textArea);
                            messageDialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                            messageDialog.setOptionType(JOptionPane.YES_NO_OPTION);
                            messageDialog.show();
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
                        ExceptionDialogOperationCancelled dialog = new ExceptionDialogOperationCancelled(this, "failed to save plot image", e);
                        dialog.show();
                    }
                }
            }
        }

        void updatePlotImage() throws IOException
        {
            BufferedImage plotImage = mChart.createBufferedImage(mPlotWidthPixels, mPlotHeightPixels);
            mPlotImage = plotImage;
            mPlotLabel.setIcon(new ImageIcon(plotImage));
        }

    }

    protected static JFreeChart generateChart(String pData, Plot pPlot) throws IOException
    {
        StringReader stringReader = new StringReader(pData);
        BufferedReader bufferedReader = new BufferedReader(stringReader);
        String line = bufferedReader.readLine();
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        String token = null;
        boolean firstToken = true;
        Vector seriesVec = new Vector();
        while(tokenizer.hasMoreTokens())
        {
            token = tokenizer.nextToken();
            if(firstToken)
            {
                if(token.startsWith("#"))
                {
                    token = token.substring(1, token.length());
                }
                firstToken = false;
            }
            else
            {
                XYSeries series = new XYSeries(token);
                seriesVec.add(series);
            }
        }
        int lineCtr = 0;
        while((line = bufferedReader.readLine()) != null)
        {
            try
            {
                readDataLine(seriesVec, line);
            }
            catch(NumberFormatException e)
            {
                throw new NumberFormatException("invalid number encountered on line: " + lineCtr);
            }
            lineCtr++;
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
        NumberAxis yAxis = new VerticalNumberAxis("populations");
        XYPlot plot = new XYPlot(seriesColl, xAxis, yAxis);
        XYToolTipGenerator toolTipGen = null;
        XYURLGenerator urlGen = null;
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES, toolTipGen, urlGen);
        plot.setRenderer(renderer);
        boolean legend = true;
        JFreeChart chart = new JFreeChart("simulation results", JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
        StringBuffer subtitleBuffer = new StringBuffer();
        TextTitle modelNameSubtitle = new TextTitle("model name: " + pPlot.mModelName);
        chart.addSubtitle(modelNameSubtitle);
        TextTitle simulatorAliasSubtitle = new TextTitle("simulator: " + pPlot.mSimulatorAlias);
        chart.addSubtitle(simulatorAliasSubtitle);
        Date simulationDate = pPlot.mSimulationDate;
        DateFormat df = DateFormat.getDateTimeInstance();
        TextTitle simulationDateTimeSubtitle = new TextTitle(df.format(simulationDate));
        chart.addSubtitle(simulationDateTimeSubtitle);
        return(chart);
    }

    private static void readDataLine(Vector pSeriesVec, String pLine) throws NumberFormatException
    {
        StringTokenizer tokenizer = new StringTokenizer(pLine, ",");
        boolean firstToken = true;
        int ctr = 0;
        Double timeValue = null;
        while(tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();
            Double value = new Double(token);
            if(firstToken)
            {
                timeValue = value;
                firstToken = false;
            }
            else
            {
                XYSeries series = (XYSeries) pSeriesVec.elementAt(ctr);
                series.add(timeValue, value);
                ctr++;
            }
        }

    }

    public void plot(String pData, String pSimulatorAlias, String pModelName, String pAppName) throws IOException, NumberFormatException
    {
        Plot plot = new Plot(pData, pSimulatorAlias, pModelName, pAppName);
        Dimension frameSize = plot.getSize();
        Point location = mFramePlacer.placeInCascadeFormat(frameSize.width, 
                                                           frameSize.height);
        plot.setLocation(location);
        plot.setVisible(true);
    }
}
