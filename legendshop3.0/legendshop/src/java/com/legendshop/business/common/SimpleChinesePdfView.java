/*
 * 
 * LegendShop 多用户商城系统
 * 
 *  版权所有,并保留所有权利。
 * 
 */
package com.legendshop.business.common;

import java.io.OutputStream;
import java.util.Iterator;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.displaytag.Messages;
import org.displaytag.exception.BaseNestableJspTagException;
import org.displaytag.exception.SeverityEnum;
import org.displaytag.export.BinaryExportView;
import org.displaytag.model.Column;
import org.displaytag.model.ColumnIterator;
import org.displaytag.model.HeaderCell;
import org.displaytag.model.Row;
import org.displaytag.model.RowIterator;
import org.displaytag.model.TableModel;
import org.displaytag.util.TagConstants;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;


/**
 * 
 * LegendShop 版权所有 2009-2011,并保留所有权利。
 * ----------------------------------------------------------------------------
 * 提示：在未取得LegendShop商业授权之前，您不能将本软件应用于商业用途，否则LegendShop将保留追究的权力。
 * ----------------------------------------------------------------------------
 * 官方网站：http://www.legendesign.net
 * ----------------------------------------------------------------------------
 */
public class SimpleChinesePdfView implements BinaryExportView
{

    /**
     * TableModel to render.
     */
    private TableModel model;

    /** export full list?. */
    private boolean exportFull;

    /** include header in export?. */
    private boolean header;

    /** decorate export?. */
    private boolean decorated;

    /**
     * This is the table, added as an Element to the PDF document. It contains all the data, needed to represent the
     * visible table into the PDF
     */
    private Table tablePDF;

    /**
     * The default font used in the document.
     */
    private Font smallFont;

    /**
	 * Sets the parameters.
	 * 
	 * @param tableModel
	 *            the table model
	 * @param exportFullList
	 *            the export full list
	 * @param includeHeader
	 *            the include header
	 * @param decorateValues
	 *            the decorate values
	 * @see org.displaytag.export.ExportView#setParameters(TableModel, boolean,
	 *      boolean, boolean)
	 */
    public void setParameters(TableModel tableModel, boolean exportFullList, boolean includeHeader,
        boolean decorateValues)
    {
        this.model = tableModel;
        this.exportFull = exportFullList;
        this.header = includeHeader;
        this.decorated = decorateValues;
    }

    /**
     * Initialize the main info holder table.
     * @throws BadElementException for errors during table initialization
     */
    protected void initTable() throws BadElementException
    {
        tablePDF = new Table(this.model.getNumberOfColumns());
        tablePDF.setDefaultVerticalAlignment(Element.ALIGN_TOP);
        tablePDF.setCellsFitPage(true);
        tablePDF.setWidth(100);

        tablePDF.setPadding(2);
        tablePDF.setSpacing(0);

       // smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, new Color(0, 0, 0));
        smallFont = FontFactory.getFont("STSong-Light","UniGB-UCS2-H", Font.DEFAULTSIZE);
    }

    /**
	 * Gets the mime type.
	 * 
	 * @return "application/pdf"
	 * @see org.displaytag.export.BaseExportView#getMimeType()
	 */
    public String getMimeType()
    {
        return "application/pdf;charset=UTF-8"; //$NON-NLS-1$
    }

    /**
     * The overall PDF table generator.
     * @throws JspException for errors during value retrieving from the table model
     * @throws BadElementException IText exception
     */
    protected void generatePDFTable() throws JspException, BadElementException
    {
        if (this.header)
        {
            generateHeaders();
        }
        tablePDF.endHeaders();
        generateRows();
    }

    /**
	 * Do export.
	 * 
	 * @param out
	 *            the out
	 * @throws JspException
	 *             the jsp exception
	 * @see org.displaytag.export.BinaryExportView#doExport(OutputStream)
	 */
    public void doExport(OutputStream out) throws JspException
    {
        try
        {
            // Initialize the table with the appropriate number of columns
            initTable();

            // Initialize the Document and register it with PdfWriter listener and the OutputStream
            Document document = new Document(PageSize.A4.rotate(), 60, 60, 40, 40);
            document.addCreationDate();
            HeaderFooter footer = new HeaderFooter(new Phrase(TagConstants.EMPTY_STRING, smallFont), true);
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_CENTER);

            PdfWriter.getInstance(document, out);

            // Fill the virtual PDF table with the necessary data
            generatePDFTable();
            document.open();
            document.setFooter(footer);
            document.add(this.tablePDF);
            document.close();

        }
        catch (Exception e)
        {
            throw new PdfGenerationException(e);
        }
    }

    /**
     * Generates the header cells, which persist on every page of the PDF document.
     * @throws BadElementException IText exception
     */
    protected void generateHeaders() throws BadElementException
    {
        Iterator iterator = this.model.getHeaderCellList().iterator();

        while (iterator.hasNext())
        {
            HeaderCell headerCell = (HeaderCell) iterator.next();

            String columnHeader = headerCell.getTitle();

            if (columnHeader == null)
            {
                columnHeader = StringUtils.capitalize(headerCell.getBeanPropertyName());
            }

            Cell hdrCell = getCell(columnHeader);
            hdrCell.setGrayFill(0.9f);
            hdrCell.setHeader(true);
            tablePDF.addCell(hdrCell);

        }
    }

    /**
     * Generates all the row cells.
     * @throws JspException for errors during value retrieving from the table model
     * @throws BadElementException errors while generating content
     */
    protected void generateRows() throws JspException, BadElementException
    {
        // get the correct iterator (full or partial list according to the exportFull field)
        RowIterator rowIterator = this.model.getRowIterator(this.exportFull);
        // iterator on rows
        while (rowIterator.hasNext())
        {
            Row row = rowIterator.next();

            // iterator on columns
            ColumnIterator columnIterator = row.getColumnIterator(this.model.getHeaderCellList());

            while (columnIterator.hasNext())
            {
                Column column = columnIterator.nextColumn();

                // Get the value to be displayed for the column
                Object value = column.getValue(this.decorated);

                Cell cell = getCell(ObjectUtils.toString(value));
                tablePDF.addCell(cell);
            }
        }
    }

    /**
     * Returns a formatted cell for the given value.
     * @param value cell value
     * @return Cell
     * @throws BadElementException errors while generating content
     */
    private Cell getCell(String value) throws BadElementException
    {
        Cell cell = new Cell(new Chunk(StringUtils.trimToEmpty(value), smallFont));
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setLeading(8);
        return cell;
    }

    /**
     * Wraps IText-generated exceptions.
     * @author Fabrizio Giustina
     * @version $Revision: 1081 $ ($Author: fgiust $)
     */
    static class PdfGenerationException extends BaseNestableJspTagException
    {

        /**
         * D1597A17A6.
         */
        private static final long serialVersionUID = 899149338534L;

        /**
         * Instantiate a new PdfGenerationException with a fixed message and the given cause.
         * @param cause Previous exception
         */
        public PdfGenerationException(Throwable cause)
        {
            super(SimpleChinesePdfView.class, Messages.getString("PdfView.errorexporting"), cause); //$NON-NLS-1$
        }

        /**
		 * Gets the severity.
		 * 
		 * @return the severity
		 * @see org.displaytag.exception.BaseNestableJspTagException#getSeverity()
		 */
        public SeverityEnum getSeverity()
        {
            return SeverityEnum.ERROR;
        }
    }
}