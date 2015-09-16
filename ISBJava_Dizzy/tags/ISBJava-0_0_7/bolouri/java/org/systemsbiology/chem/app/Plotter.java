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

public class Plotter 
{
    private Component mMainFrame;

    public Plotter(Component pMainFrame)
    {
        mMainFrame = pMainFrame;
    }

    private void readDataLine(Vector pSeriesVec, String pLine) throws NumberFormatException
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

    public void plot(String pData) throws IOException
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
        while((line = bufferedReader.readLine()) != null)
        {
            readDataLine(seriesVec, line);
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
        BufferedImage chartImage = chart.createBufferedImage(500,300);
        JLabel chartLabel = new JLabel();
        chartLabel.setIcon(new ImageIcon(chartImage));
        JFrame chartFrame = new JFrame("simulation plot");
        JPanel chartPanel = new JPanel();
        chartPanel.add(chartLabel);
        chartFrame.getContentPane().add(chartPanel);
        chartFrame.pack();
        chartFrame.setVisible(true);
        
    }
}
