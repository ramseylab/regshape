package org.systemsbiology.gui;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

/**
 * Implements data transfer for an image to the
 * system clipboard.
 * 
 * Obtained from Java tutorials on the Internet.
 */
import java.awt.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.*;
import org.systemsbiology.math.*;

public class ImageTransferHandler extends TransferHandler {


    private static final int IMAGE_SIZE_TEST = 10;

    private static final DataFlavor flavors[];

    static
    {
        String []formatNames = ImageIO.getWriterFormatNames();
        String []mimeTypes = ImageIO.getWriterMIMETypes();
        int numMimeTypes = mimeTypes.length;
        HashMap dataFlavors = new HashMap();
        DataFlavor imageDataFlavor = DataFlavor.imageFlavor;
        dataFlavors.put(imageDataFlavor.getMimeType(), imageDataFlavor);
        for(int i = 0; i < numMimeTypes; ++i)
        {
            String mimeType = mimeTypes[i];
            if(null == dataFlavors.get(mimeType))
            {
                try
                {
                    DataFlavor newDataFlavor = new DataFlavor(mimeType);
                    dataFlavors.put(mimeType, newDataFlavor);
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        DataFlavor []fake = new DataFlavor[0];
        flavors = (DataFlavor []) dataFlavors.values().toArray(fake);
    }

    public int getSourceActions(JComponent c) 
    { 
        return TransferHandler.COPY; 
    } 

    public boolean canImport(
        JComponent comp, DataFlavor flavor[]) {
        if (!(comp instanceof JLabel)) {
            return false;
        }
        for (int i=0, n=flavor.length; i<n; i++) {
            for (int j=0, m=flavors.length; j<m; j++) {
                if (flavor[i].equals(flavors[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkDoesSystemClipboardSupportImageTransfer()
    {
        JLabel testLabel = new JLabel();
        BufferedImage bufImg = new BufferedImage(IMAGE_SIZE_TEST, IMAGE_SIZE_TEST, BufferedImage.TYPE_INT_RGB);
        ImageIcon imageIcon = new ImageIcon(bufImg);
        testLabel.setIcon(imageIcon);
        MutableBoolean testSucceeded = new MutableBoolean(false);
        Transferable transferable = createTransferableImage(IMAGE_SIZE_TEST, IMAGE_SIZE_TEST, imageIcon.getImage(), testSucceeded);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
        return(testSucceeded.getValue());
    }

    private static Transferable createTransferableImage(int pHeight, int pWidth, Image pImage, MutableBoolean pTransferredSuccessfully)
    {
        final Image image = pImage;
        final int height = pHeight;
        final int width = pWidth;
        final MutableBoolean transferredSuccessfully = pTransferredSuccessfully;

        Transferable transferable = 
            new Transferable() 
            {
                public Object getTransferData(DataFlavor flavor) 
                {
                    Object retObj = null;
                    if (isDataFlavorSupported(flavor)) 
                    {
                        if(flavor.equals(DataFlavor.imageFlavor))
                        {
                            retObj = image;
                        }
                        else
                        {
                            // create a byte array
                            String mimeType = flavor.getMimeType();
                            String format = flavor.getSubType();

                            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                            Graphics2D bufferedImageContext = bufferedImage.createGraphics();
                            bufferedImageContext.drawImage(image, null, null);
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            try
                            {
                                ImageIO.write(bufferedImage, format, byteArrayOutputStream);
                            }
                            catch(Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                            retObj = byteArrayOutputStream;
                        }
                    }
                    if(null != transferredSuccessfully)
                    {
                        if(! transferredSuccessfully.getValue())
                        {
                            boolean success = (null != retObj);
                            transferredSuccessfully.setValue(null != retObj);
                        }
                    }
                    return(retObj);
                }

                public boolean isDataFlavorSupported(DataFlavor flavor) 
                {
                    return flavor.equals(
                        DataFlavor.imageFlavor);
                }


                public DataFlavor[] getTransferDataFlavors() 
                {
                    return flavors;
                }
            };
        return transferable;
    }

    public Transferable createTransferable(JComponent comp) 
    {
        if (comp instanceof JLabel) 
        {
            JLabel label = (JLabel)comp;
            Icon icon = label.getIcon();
            if (icon instanceof ImageIcon) 
            {
                final Image image = ((ImageIcon)icon).getImage();
                final JLabel source = label;
                Transferable transferable = createTransferableImage(source.getHeight(), 
                                                                    source.getWidth(),
                                                                    image,
                                                                    null);
                return transferable;
            }
        }
        return null;
    }
}
