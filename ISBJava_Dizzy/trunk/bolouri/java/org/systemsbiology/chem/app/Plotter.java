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

public class Plotter 
{
    private Component mMainFrame;
    private static final int DEFAULT_WIDTH_PIXELS = 500;
    private static final int DEFAULT_HEIGHT_PIXELS = 400;
    public static final int MAX_NUM_SPECIES_TO_PLOT = 20;
    private static final int PLOT_POSITION_STAGGER_AMOUNT_PIXELS = 20;

    private static int sPlotCtr;

    static
    {
        sPlotCtr = 0;
    }

    static class Plot extends JFrame
    {
        String mSimulatorAlias;
        String mModelName;
        Date mSimulationDate;
        int mHeightPixels;
        int mWidthPixels;
        JLabel mPlotLabel;
        JFreeChart mChart;
        
        public Plot(String pAppName, String pData, String pSimulatorAlias, String pModelName) throws IOException
        {
            super(pAppName + ": results");
            mSimulatorAlias = pSimulatorAlias;
            mModelName = pModelName;
            mSimulationDate = new Date(System.currentTimeMillis());
            mHeightPixels = DEFAULT_HEIGHT_PIXELS;
            mWidthPixels = DEFAULT_WIDTH_PIXELS;

            JPanel plotPanel = new JPanel();
            JLabel plotLabel = new JLabel();
            plotPanel.add(plotLabel);
            mPlotLabel = plotLabel;

            addComponentListener(new ComponentAdapter()
            {
                public void componentResized(ComponentEvent e) 
                {
                    mHeightPixels = getHeight();
                    mWidthPixels = getWidth();
                    try
                    {
                        updatePlotImage();
                    }
                    catch(IOException e2)
                    {
                        e2.printStackTrace(System.err); // :BUGBUG: what to do here?
                    }
                }

            });

            mChart = generateChart(pData, this);
            updatePlotImage();
            
            getContentPane().add(plotPanel);
            pack();
        }
        
        void updatePlotImage() throws IOException
        {
            BufferedImage plotImage = mChart.createBufferedImage(mWidthPixels, mHeightPixels);
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
        TextTitle simulatorAliasSubtitle = new TextTitle("simulator alias: " + pPlot.mSimulatorAlias);
        chart.addSubtitle(simulatorAliasSubtitle);
        Date simulationDate = pPlot.mSimulationDate;
        DateFormat df = DateFormat.getDateTimeInstance();
        TextTitle simulationDateTimeSubtitle = new TextTitle(df.format(simulationDate));
        chart.addSubtitle(simulationDateTimeSubtitle);
        return(chart);
    }

    public Plotter(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
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

    public static void plot(String pData, String pSimulatorAlias, String pModelName, String pAppName) throws IOException, NumberFormatException
    {
        Plot plot = new Plot(pData, pSimulatorAlias, pModelName, pAppName);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = plot.getSize();
        int numPixelsStagger = sPlotCtr * PLOT_POSITION_STAGGER_AMOUNT_PIXELS;
        plot.setLocation((screenSize.width - frameSize.width) / 4 + numPixelsStagger,
                         (screenSize.height - frameSize.height) / 4 + numPixelsStagger);
        if(numPixelsStagger < (screenSize.height - frameSize.height)/4)
        {
            ++sPlotCtr;
        }
        else
        {
            sPlotCtr = 0;
        }
        plot.setVisible(true);
    }
}
