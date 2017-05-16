/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2016 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.engine.fill;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRGroup;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.type.FooterPositionEnum;
import net.sf.jasperreports.engine.type.IncrementTypeEnum;
import net.sf.jasperreports.engine.type.ResetTypeEnum;
import net.sf.jasperreports.engine.type.RunDirectionEnum;


/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class JRHorizontalFiller extends JRBaseFiller
{

	private static final Log log = LogFactory.getLog(JRHorizontalFiller.class);

	private int lastDetailOffsetX = -1;
	private int lastDetailOffsetY = -1;
	private int currentDetailOffsetY;
	private int maxDetailOffsetY;
	

	/**
	 *
	 */
	protected JRHorizontalFiller(
		JasperReportsContext jasperReportsContext, 
		JasperReport jasperReport
		) throws JRException
	{
		this(jasperReportsContext, jasperReport, null);
	}

	/**
	 *
	 */
	public JRHorizontalFiller(
		JasperReportsContext jasperReportsContext, 
		JasperReport jasperReport, 
		BandReportFillerParent parent 
		) throws JRException
	{
		super(jasperReportsContext, jasperReport, parent);

		setPageHeight(pageHeight);
	}


	@Override
	protected void setPageHeight(int pageHeight)
	{
		this.pageHeight = pageHeight;

		columnFooterOffsetY = pageHeight - bottomMargin - pageFooter.getHeight() - columnFooter.getHeight();
		lastPageColumnFooterOffsetY = pageHeight - bottomMargin - lastPageFooter.getHeight() - columnFooter.getHeight();
		
		if (log.isDebugEnabled())
		{
			log.debug("Filler " + fillerId + " - pageHeight: " + pageHeight
					+ ", columnFooterOffsetY: " + columnFooterOffsetY
					+ ", lastPageColumnFooterOffsetY: " + lastPageColumnFooterOffsetY);
		}
	}


	@Override
	protected synchronized void fillReport() throws JRException
	{
		setLastPageFooter(false);

		if (next())
		{
			fillReportStart();

			while (next())
			{
				fillReportContent();
			}

			fillReportEnd();
		}
		else
		{
			if (log.isDebugEnabled())
			{
				log.debug("Fill " + fillerId + ": no data");
			}

			switch (getWhenNoDataType())
			{
				case ALL_SECTIONS_NO_DETAIL :
				{
					if (log.isDebugEnabled())
					{
						log.debug("Fill " + fillerId + ": all sections");
					}

					scriptlet.callBeforeReportInit();
					calculator.initializeVariables(ResetTypeEnum.REPORT, IncrementTypeEnum.REPORT);
					scriptlet.callAfterReportInit();

					printPage = newPage();
					addPage(printPage);
					setFirstColumn();
					offsetY = topMargin;

					fillBackground();

					fillTitle();

					fillPageHeader(JRExpression.EVALUATION_DEFAULT);

					fillColumnHeaders(JRExpression.EVALUATION_DEFAULT);

					fillGroupHeaders(true);

					fillGroupFooters(true);

					fillSummary();

					break;
				}
				case BLANK_PAGE :
				{
					if (log.isDebugEnabled())
					{
						log.debug("Fill " + fillerId + ": blank page");
					}

					printPage = newPage();
					addPage(printPage);
					break;
				}
				case NO_DATA_SECTION:
				{
					if (log.isDebugEnabled())
					{
						log.debug("Fill " + fillerId + ": NoData section");
					}

					scriptlet.callBeforeReportInit();
					calculator.initializeVariables(ResetTypeEnum.REPORT, IncrementTypeEnum.REPORT);
					scriptlet.callAfterReportInit();

					printPage = newPage();
					addPage(printPage);
					setFirstColumn();
					offsetY = topMargin;

					fillBackground();

					fillNoData();

					break;

				}
				case NO_PAGES :
				default :
				{
					if (log.isDebugEnabled())
					{
						log.debug("Fill " + fillerId + ": no pages");
					}
				}
			}
		}

		if (ignorePagination)
		{
			jasperPrint.setPageHeight(offsetY + bottomMargin);
		}

		if (isSubreport())
		{
			addPageToParent(true);
		}
		else
		{
			addLastPageBookmarks();
		}
		
		if (bookmarkHelper != null)
		{
			jasperPrint.setBookmarks(bookmarkHelper.getRootBookmarks());
		}
	}


	/**
	 *
	 */
	private void fillReportStart() throws JRException
	{
		scriptlet.callBeforeReportInit();
		calculator.initializeVariables(ResetTypeEnum.REPORT, IncrementTypeEnum.REPORT);
		scriptlet.callAfterReportInit();

		printPage = newPage();
		addPage(printPage);
		setFirstColumn();
		offsetY = topMargin;

		fillBackground();

		fillTitle();

		fillPageHeader(JRExpression.EVALUATION_DEFAULT);

		fillColumnHeaders(JRExpression.EVALUATION_DEFAULT);

		fillGroupHeaders(true);

		fillDetail();
	}


	private void setFirstColumn()
	{
		columnIndex = 0;
		offsetX = leftMargin;
		setColumnNumberVariable();
	}

	/**
	 *
	 */
	private void fillReportContent() throws JRException
	{
		calculator.estimateGroupRuptures();

		fillGroupFooters(false);

		resolveGroupBoundElements(JRExpression.EVALUATION_OLD, false);
		scriptlet.callBeforeGroupInit();
		calculator.initializeVariables(ResetTypeEnum.GROUP, IncrementTypeEnum.GROUP);
		scriptlet.callAfterGroupInit();

		fillGroupHeaders(false);

		fillDetail();
	}


	/**
	 *
	 */
	private void fillReportEnd() throws JRException
	{
		fillGroupFooters(true);

		fillSummary();
	}


	/**
	 *
	 */
	 private void fillTitle() throws JRException
	 {
		if (log.isDebugEnabled() && !title.isEmpty())
		{
			log.debug("Fill " + fillerId + ": title at " + offsetY);
		}

		title.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

		if (title.isToPrint())
		{
			while (
				title.getBreakHeight() > pageHeight - bottomMargin - offsetY
				)
			{
				addPage(false);
			}

			title.evaluate(JRExpression.EVALUATION_DEFAULT);

			JRPrintBand printBand = title.fill(pageHeight - bottomMargin - offsetY);

			if (title.willOverflow() && title.isSplitPrevented() && isSubreport())
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				printBand = title.refill(pageHeight - bottomMargin - offsetY);
			}

			fillBand(printBand);
			offsetY += printBand.getHeight();

			while (title.willOverflow())
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				printBand = title.fill(pageHeight - bottomMargin - offsetY);

				fillBand(printBand);
				offsetY += printBand.getHeight();
			}

			resolveBandBoundElements(title, JRExpression.EVALUATION_DEFAULT);

			if (isTitleNewPage)
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);
			}
		}
	}


	/**
	 *
	 */
	private void fillPageHeader(byte evaluation) throws JRException
	{
		if (log.isDebugEnabled() && !pageHeader.isEmpty())
		{
			log.debug("Fill " + fillerId + ": page header at " + offsetY);
		}

		setNewPageColumnInBands();

		pageHeader.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

		if (pageHeader.isToPrint())
		{
			int reattempts = getMasterColumnCount();
			if (isCreatingNewPage)
			{
				--reattempts;
			}

			boolean filled = fillBandNoOverflow(pageHeader, evaluation);

			for (int i = 0; !filled && i < reattempts; ++i)
			{
				resolveGroupBoundElements(evaluation, false);
				resolveColumnBoundElements(evaluation);
				resolvePageBoundElements(evaluation);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				filled = fillBandNoOverflow(pageHeader, evaluation);
			}

			if (!filled)
			{
				throw 
					new JRRuntimeException(
						EXCEPTION_MESSAGE_KEY_PAGE_HEADER_OVERFLOW_INFINITE_LOOP,
						(Object[])null);
			}
		}

		columnHeaderOffsetY = offsetY;

		isNewPage = true;
		isFirstPageBand = true;
	}


	private boolean fillBandNoOverflow(JRFillBand band, byte evaluation) throws JRException
	{
		int availableHeight = columnFooterOffsetY - offsetY;
		boolean overflow = availableHeight < band.getHeight();

		if (!overflow)
		{
			band.evaluate(evaluation);
			JRPrintBand printBand = band.fill(availableHeight);

			overflow = band.willOverflow();
			if (overflow)
			{
				band.rewind();
			}
			else
			{
				fillBand(printBand);
				offsetY += printBand.getHeight();

				resolveBandBoundElements(band, evaluation);
			}
		}

		return !overflow;
	}


	/**
	 *
	 */
	private void fillColumnHeaders(byte evaluation) throws JRException
	{
		if (log.isDebugEnabled() && !columnHeader.isEmpty())
		{
			log.debug("Fill " + fillerId + ": column headers at " + offsetY);
		}

		setNewPageColumnInBands();

		for(columnIndex = 0; columnIndex < columnCount; columnIndex++)
		{
			setColumnNumberVariable();

			columnHeader.evaluatePrintWhenExpression(evaluation);

			if (columnHeader.isToPrint())
			{
				int reattempts = getMasterColumnCount();
				if (isCreatingNewPage)
				{
					--reattempts;
				}

				boolean fits = columnHeader.getHeight() <= columnFooterOffsetY - offsetY;
				for (int i = 0; !fits && i < reattempts; ++i)
				{
					fillPageFooter(evaluation);

					resolveGroupBoundElements(evaluation, false);
					resolveColumnBoundElements(evaluation);
					resolvePageBoundElements(evaluation);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);

					fillPageHeader(evaluation);

					fits = columnHeader.getHeight() <= columnFooterOffsetY - offsetY;
				}

				if (!fits)
				{
					throw 
						new JRRuntimeException(
							EXCEPTION_MESSAGE_KEY_COLUMN_HEADER_OVERFLOW_INFINITE_LOOP,
							(Object[])null);
				}

				setOffsetX();
				offsetY = columnHeaderOffsetY;

				fillFixedBand(columnHeader, evaluation, false);
			}
		}

		setFirstColumn();

		isNewColumn = true;
		isFirstColumnBand = true;
	}


	/**
	 *
	 */
	private void fillGroupHeaders(boolean isFillAll) throws JRException
	{
		if (groups != null && groups.length > 0)
		{
			for (int i = 0; i < groups.length; i++)
			{
				JRFillGroup group = groups[i];

				if (isFillAll || group.hasChanged())
				{
					fillGroupHeader(group);
				}
			}
		}
	}


	/**
	 *
	 */
	private void fillGroupHeader(JRFillGroup group) throws JRException
	{
		JRFillSection groupHeaderSection = (JRFillSection)group.getGroupHeaderSection();

		if (log.isDebugEnabled() && !groupHeaderSection.isEmpty())
		{
			log.debug("Fill " + fillerId + ": " + group.getName() + " header at " + offsetY);
		}

		byte evalPrevPage = (group.isTopLevelChange()?JRExpression.EVALUATION_OLD:JRExpression.EVALUATION_DEFAULT);

		if (
			(group.isStartNewPage() || group.isResetPageNumber()) && !isNewPage
			|| ( group.isStartNewColumn() && !isNewColumn )
			)
		{
			fillPageBreak(
				group.isResetPageNumber(),
				evalPrevPage,
				JRExpression.EVALUATION_DEFAULT,
				true
				);
		}

		JRFillBand[] groupHeaderBands = groupHeaderSection.getFillBands();
		for(int i = 0; i < groupHeaderBands.length; i++)
		{
			JRFillBand groupHeaderBand = groupHeaderBands[i];

			groupHeaderBand.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (groupHeaderBand.isToPrint())
			{
				while (
					groupHeaderBand.getBreakHeight() > columnFooterOffsetY - offsetY ||
					group.getMinHeightToStartNewPage() > columnFooterOffsetY - offsetY
					)
				{
					fillPageBreak(
						false,
						evalPrevPage,
						JRExpression.EVALUATION_DEFAULT,
						true
						);
				}
			}

			if (i == 0)
			{
				setNewGroupInBands(group);

				group.setFooterPrinted(false);
			}

			ElementRange elementRange = null;
			
			if (group.isKeepTogether())
			{
				elementRange = group.getKeepTogetherElementRange();
				
				if (elementRange == null && !isNewColumn)
				{
					// we need to set a keep together element range for the group
					// even if its header does not print,
					// but only if the column is not already new
					elementRange = 
						new SimpleElementRange(
							getCurrentPage(), 
							columnIndex, 
							offsetY
							);
					group.setKeepTogetherElementRange(elementRange);
					// setting a non-null element range here would cause the group header band to be
					// refilled below and thus kept together, in case a split occurs in it;
					// the non-null element range will be also moved onto the new page/column in the process,
					// but it will contain no elements as the already mentioned non-splitting behavior of the group header band
					// would not add any element to it;
					// so the keep together element range set here is more like flag to signal the group header itself
					// should be prevented from splitting in the fillColumnBand call below
				}
			}

			if (groupHeaderBand.isToPrint())
			{
				setFirstColumn();

				ElementRange newElementRange = fillColumnBand(groupHeaderBand, JRExpression.EVALUATION_DEFAULT);
				
				// in case a column/page break occurred during the filling of the band above,
				// the provided element range is discarded (we don't capture the return of the expand method below),
				// but that should not be a problem because the discarded element range was already dealt with during the break, 
				// because it was a keep together element range
				ElementRangeUtil.expand(elementRange, newElementRange);

				isFirstPageBand = false;
				isFirstColumnBand = true;
			}
		}

		group.setHeaderPrinted(true);

		isNewGroup = true;
	}


	/**
	 *
	 */
	private void fillGroupHeadersReprint(byte evaluation) throws JRException
	{
		if (groups != null && groups.length > 0)
		{
			for (int i = 0; i < groups.length; i++)
			{
				JRFillGroup group = groups[i];
				
				if (group.getKeepTogetherElementRange() != null)
				{
					// we reprint headers only for groups that are "outer" to the one which 
					// triggered a potential "keep together" move 
					break;
				}

				if (
					group.isReprintHeaderOnEachPage()
					&& (!group.hasChanged() || (group.hasChanged() && group.isHeaderPrinted()))
					)
				{
					fillGroupHeaderReprint(groups[i], evaluation);
				}
			}
		}
	}


	/**
	 *
	 */
	 private void fillGroupHeaderReprint(JRFillGroup group, byte evaluation) throws JRException
	 {
		JRFillSection groupHeaderSection = (JRFillSection)group.getGroupHeaderSection();

		JRFillBand[] groupHeaderBands = groupHeaderSection.getFillBands();
		for (int i = 0; i < groupHeaderBands.length; i++)
		{
			JRFillBand groupHeaderBand = groupHeaderBands[i];

			groupHeaderBand.evaluatePrintWhenExpression(evaluation);

			if (groupHeaderBand.isToPrint())
			{
				setFirstColumn();

				while (
					groupHeaderBand.getBreakHeight() > columnFooterOffsetY - offsetY ||
					group.getMinHeightToStartNewPage() > columnFooterOffsetY - offsetY
					)
				{
					fillPageBreak(false, evaluation, evaluation, true);
				}

				fillColumnBand(groupHeaderBand, evaluation);

				isFirstPageBand = false;
				isFirstColumnBand = true;
			}
		}
	}


	/**
	 *
	 */
	private void fillDetail() throws JRException
	{
		if (log.isDebugEnabled() && !detailSection.isEmpty())
		{
			log.debug("Fill " + fillerId + ": detail at " + offsetY);
		}

		if (!detailSection.areAllPrintWhenExpressionsNull())
		{
			calculator.estimateVariables();
		}

		JRFillBand[] detailBands = detailSection.getFillBands();
		for (int i = 0; i < detailBands.length; i++)
		{
			JRFillBand detailBand = detailBands[i];

			detailBand.evaluatePrintWhenExpression(JRExpression.EVALUATION_ESTIMATED);

			if (detailBand.isToPrint())
			{
				while (
					(columnIndex == columnCount - 1 || isNewGroup)
					&& detailBand.getHeight() > columnFooterOffsetY - offsetY
					)
				{
					byte evalPrevPage = (isNewGroup?JRExpression.EVALUATION_DEFAULT:JRExpression.EVALUATION_OLD);

					fillPageBreak(
						false,
						evalPrevPage,
						JRExpression.EVALUATION_DEFAULT,
						true
						);
				}
				
				break;
			}
		}

		scriptlet.callBeforeDetailEval();
		calculator.calculateVariables();
		scriptlet.callAfterDetailEval();
				
		if (
			offsetX == lastDetailOffsetX
			&& offsetY == lastDetailOffsetY
			)
		{
			if (columnIndex == columnCount - 1)
			{
				columnIndex = 0;
				setOffsetX();
				setColumnNumberVariable();

				maxDetailOffsetY = 0;
				currentDetailOffsetY = offsetY;
			}
			else
			{
				columnIndex++;
				setOffsetX();
				offsetY = currentDetailOffsetY;

				setColumnNumberVariable();
			}
		}
		else
		{
			setOffsetX();
			currentDetailOffsetY = offsetY;
		}

		for (int i = 0; i < detailBands.length; i++)
		{
			JRFillBand detailBand = detailBands[i];

			detailBand.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (detailBand.isToPrint())
			{
				while (
					//(columnIndex == columnCount - 1 || isNewGroup) &&
					detailBand.getHeight() > columnFooterOffsetY - offsetY
					)
				{
					byte evalPrevPage = (isNewGroup?JRExpression.EVALUATION_DEFAULT:JRExpression.EVALUATION_OLD);

					fillPageBreak(
						false,
						evalPrevPage,
						JRExpression.EVALUATION_DEFAULT,
						true
						);

					currentDetailOffsetY = offsetY;
				}
				
				fillFixedBand(detailBand, JRExpression.EVALUATION_DEFAULT, false);

				isFirstPageBand = false;
				isFirstColumnBand = false;
			}
		}

		maxDetailOffsetY = maxDetailOffsetY < offsetY ? offsetY : maxDetailOffsetY;
		offsetY = maxDetailOffsetY;

		lastDetailOffsetX = offsetX;
		lastDetailOffsetY = offsetY;
		
		isNewPage = false;
		isNewColumn = false;
		isNewGroup = false;
	}


	/**
	 *
	 */
	private void fillGroupFooters(boolean isFillAll) throws JRException
	{
		if (groups != null && groups.length > 0)
		{
			byte evaluation = (isFillAll)?JRExpression.EVALUATION_DEFAULT:JRExpression.EVALUATION_OLD;

			for(int i = groups.length - 1; i >= 0; i--)
			{
				JRFillGroup group = groups[i];
				
				if (isFillAll || group.hasChanged())
				{
					fillGroupFooter(group, evaluation);
					
					// regardless of whether the fillGroupFooter was printed or not, 
					// we just need to mark the end of the group 
					group.setKeepTogetherElementRange(null);
				}
			}
			
			// we need to take care of groupFooterPositionElementRange here because all groups footers have been 
			// rendered and we need to consume remaining space before next groups start;
			//
			// but we don't process the last groupFooterPositionElementRange when the report ends (isFillAll true),
			// because it will be dealt with during summary rendering, depending on whether a last page footer exists or not
			if (
				!isFillAll
				&& groupFooterPositionElementRange != null
				)
			{
				ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
				groupFooterPositionElementRange = null;
				// update the offsetY to signal there is no more space left at the bottom after forcing the footer
				offsetY = columnFooterOffsetY;
			}
		}
	}


	/**
	 *
	 */
	private void fillGroupFooter(JRFillGroup group, byte evaluation) throws JRException
	{
		JRFillSection groupFooterSection = (JRFillSection)group.getGroupFooterSection();

		if (log.isDebugEnabled() && !groupFooterSection.isEmpty())
		{
			log.debug("Fill " + fillerId + ": " + group.getName() + " footer at " + offsetY);
		}
		
		JRFillBand[] groupFooterBands = groupFooterSection.getFillBands();
		for (int i = 0; i < groupFooterBands.length; i++)
		{
			JRFillBand groupFooterBand = groupFooterBands[i];
			
			groupFooterBand.evaluatePrintWhenExpression(evaluation);

			if (groupFooterBand.isToPrint())
			{
				setFirstColumn();

				if (
					groupFooterBand.getBreakHeight() > columnFooterOffsetY - offsetY
					)
				{
					fillPageBreak(false, evaluation, evaluation, true);
				}

				ElementRange newElementRange = fillColumnBand(groupFooterBand, evaluation);

				if (groupFooterPositionElementRange == null)
				{
					if (group.getFooterPositionValue() != FooterPositionEnum.NORMAL)
					{
						groupFooterPositionElementRange = new SimpleGroupFooterElementRange(newElementRange, group.getFooterPositionValue());
					}
				}
				else
				{
					switch (group.getFooterPositionValue())
					{
						case STACK_AT_BOTTOM :
						{
							groupFooterPositionElementRange.getElementRange().expand(newElementRange.getBottomY());
							groupFooterPositionElementRange.setFooterPosition(FooterPositionEnum.STACK_AT_BOTTOM);
							break;
						}
						case FORCE_AT_BOTTOM :
						{
							groupFooterPositionElementRange.getElementRange().expand(newElementRange.getBottomY());
							groupFooterPositionElementRange.setFooterPosition(FooterPositionEnum.FORCE_AT_BOTTOM);
							break;
						}
						case COLLATE_AT_BOTTOM :
						{
							groupFooterPositionElementRange.getElementRange().expand(newElementRange.getBottomY());
							break;
						}
						case NORMAL :
						default :
						{
							// only StackAtBottom and CollateAtBottom can get here
							if (groupFooterPositionElementRange.getFooterPosition() == FooterPositionEnum.STACK_AT_BOTTOM)
							{
								groupFooterPositionElementRange.getElementRange().expand(newElementRange.getBottomY());
							}
							else
							{
								groupFooterPositionElementRange = null;
							}
							break;
						}
					}
				}

				isFirstPageBand = false;
				isFirstColumnBand = true;
			}
		}

		// we need to perform ForceAtBottom here because only the group footer as a whole should be forced to bottom, 
		// not the individual bands in this footer section;
		// also, when forcing a group footer to bottom, we consider the normal/current columnFooterOffsetY, because it is impossible
		// to tell at this point if this would be the last page or not (last page footer)
		if (
			groupFooterPositionElementRange != null
			&& groupFooterPositionElementRange.getFooterPosition() == FooterPositionEnum.FORCE_AT_BOTTOM
			)
		{
			ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
			groupFooterPositionElementRange = null;
			// update the offsetY to signal there is no more space left at the bottom after forcing the footer
			offsetY = columnFooterOffsetY;
		}

		isNewPage = false;
		isNewColumn = false;

		group.setHeaderPrinted(false);
		group.setFooterPrinted(true);
	}


	/**
	 *
	 */
	 private void fillColumnFooters(byte evaluation) throws JRException
	 {
		if (log.isDebugEnabled() && !columnFooter.isEmpty())
		{
			log.debug("Fill " + fillerId + ": column footers at " + offsetY);
		}

		/*
		if (!isSubreport)
		{
			offsetY = columnFooterOffsetY;
		}
		*/

		if (isSubreport() && !isSubreportRunToBottom())
		{
			columnFooterOffsetY = offsetY;
		}

		int tmpColumnFooterOffsetY = columnFooterOffsetY;

		if (isFloatColumnFooter || ignorePagination)
		{
			tmpColumnFooterOffsetY = offsetY;
		}

		// we first let the column footer Y offset calculations to occur normally above, 
		// before attempting to deal with existing groupFooterPositionElementRange
		if (groupFooterPositionElementRange != null)
		{
			// all types of footer position can get here (StackAtBottom, CollapseAtBottom and ForceAtBottom);
			// ForceAtBottom group footer element ranges could reach this point in case multi-band group footer gets
			// split across a column/page break; remaining bands in such group footer would be dealt at the end 
			// of the group footer filling method (see fillGroupFooter() method above)
			ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
			groupFooterPositionElementRange = null;
			// we do not need to set the offsetY because it has already been set properly earlier in this method;
		}
		
		for (columnIndex = 0; columnIndex < columnCount; columnIndex++)
		{
			setColumnNumberVariable();

			setOffsetX();
			offsetY = tmpColumnFooterOffsetY;

			columnFooter.evaluatePrintWhenExpression(evaluation);

			if (columnFooter.isToPrint())
			{
				fillFixedBand(columnFooter, evaluation, false);
			}
		}
	}


	/**
	 *
	 */
	private void fillPageFooter(byte evaluation) throws JRException
	{
		JRFillBand crtPageFooter = getCurrentPageFooter();

		if (log.isDebugEnabled() && !crtPageFooter.isEmpty())
		{
			log.debug("Fill " + fillerId + ": " + (isLastPageFooter ? "last " : "") + "page footer at " + offsetY);
		}

		offsetX = leftMargin;

		if ((!isSubreport() || isSubreportRunToBottom()) && !ignorePagination)
		{
			offsetY = pageHeight - crtPageFooter.getHeight() - bottomMargin;
		}

		crtPageFooter.evaluatePrintWhenExpression(evaluation);

		if (crtPageFooter.isToPrint())
		{
			fillFixedBand(crtPageFooter, evaluation);
		}
	}


	/**
	 *
	 */
	private void fillSummary() throws JRException
	{
		if (log.isDebugEnabled() && !summary.isEmpty())
		{
			log.debug("Fill " + fillerId + ": summary at " + offsetY);
		}

		offsetX = leftMargin;

		if (lastPageFooter == missingFillBand)
		{
			if (
				!isSummaryNewPage
				//&& columnIndex == 0
				&& summary.getBreakHeight() <= columnFooterOffsetY - offsetY
				)
			{
				fillSummaryNoLastFooterSamePage();
			}
			else
			{
				fillSummaryNoLastFooterNewPage();
			}
		}
		else
		{
			if (isSummaryWithPageHeaderAndFooter)
			{
				fillSummaryWithLastFooterAndPageBands();
			}
			else
			{
				fillSummaryWithLastFooterNoPageBands();
			}
		}

		resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
		resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
		resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
		resolveReportBoundElements();
		if (isMasterReport())
		{
			resolveMasterBoundElements();
		}
	}


	/**
	 *
	 */
	private void fillSummaryNoLastFooterSamePage() throws JRException
	{
		summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

		if (summary != missingFillBand && summary.isToPrint())
		{
			// deal with groupFooterPositionElementRange here because summary will attempt to use remaining space
			if (groupFooterPositionElementRange != null)
			{
				ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
				offsetY = columnFooterOffsetY;
				// reset the element range here although it will not be checked anymore as the report ends
				groupFooterPositionElementRange = null;
			}
			
			summary.evaluate(JRExpression.EVALUATION_DEFAULT);

			JRPrintBand printBand = summary.fill(columnFooterOffsetY - offsetY);

			if (summary.willOverflow() && summary.isSplitPrevented())
			{
				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);

				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);
				
				if (isSummaryWithPageHeaderAndFooter)
				{
					fillPageHeader(JRExpression.EVALUATION_DEFAULT);
				}

				printBand = summary.refill(pageHeight - bottomMargin - offsetY - (isSummaryWithPageHeaderAndFooter?pageFooter.getHeight():0));

				fillBand(printBand);
				offsetY += printBand.getHeight();

				/*   */
				fillSummaryOverflow();
				
				//DONE
			}
			else
			{
				fillBand(printBand);
				offsetY += printBand.getHeight();

				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				if (summary.willOverflow())
				{
					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);
					
					if (isSummaryWithPageHeaderAndFooter)
					{
						fillPageHeader(JRExpression.EVALUATION_DEFAULT);
					}

					printBand = summary.fill(pageHeight - bottomMargin - offsetY - (isSummaryWithPageHeaderAndFooter?pageFooter.getHeight():0));

					fillBand(printBand);
					offsetY += printBand.getHeight();

					/*   */
					fillSummaryOverflow();
					
					//DONE
				}
				else
				{
					resolveBandBoundElements(summary, JRExpression.EVALUATION_DEFAULT);

					//DONE
				}
			}
		}
		else
		{
			// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
			
			fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

			fillPageFooter(JRExpression.EVALUATION_DEFAULT);
			
			//DONE
		}
	}


	/**
	 *
	 */
	private void fillSummaryNoLastFooterNewPage() throws JRException
	{
		// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
		
		fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

		fillPageFooter(JRExpression.EVALUATION_DEFAULT);

		summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

		if (summary != missingFillBand && summary.isToPrint())
		{
			resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
			resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
			resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
			scriptlet.callBeforePageInit();
			calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
			scriptlet.callAfterPageInit();

			addPage(false);

			if (isSummaryWithPageHeaderAndFooter)
			{
				fillPageHeader(JRExpression.EVALUATION_DEFAULT);
			}

			summary.evaluate(JRExpression.EVALUATION_DEFAULT);

			JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY - (isSummaryWithPageHeaderAndFooter?pageFooter.getHeight():0));

			if (summary.willOverflow() && summary.isSplitPrevented() && isSubreport())
			{
				if (isSummaryWithPageHeaderAndFooter)
				{
					fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				}

				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				if (isSummaryWithPageHeaderAndFooter)
				{
					fillPageHeader(JRExpression.EVALUATION_DEFAULT);
				}

				printBand = summary.refill(pageHeight - bottomMargin - offsetY - (isSummaryWithPageHeaderAndFooter?pageFooter.getHeight():0));
			}

			fillBand(printBand);
			offsetY += printBand.getHeight();

			/*   */
			fillSummaryOverflow();
		}
		
		//DONE
	}


	/**
	 *
	 */
	private void fillSummaryWithLastFooterAndPageBands() throws JRException
	{
		if (
			!isSummaryNewPage
			//&& columnIndex == 0
			&& summary.getBreakHeight() <= columnFooterOffsetY - offsetY
			)
		{
			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				// deal with groupFooterPositionElementRange here because summary will attempt to use remaining space
				if (groupFooterPositionElementRange != null)
				{
					ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
					offsetY = columnFooterOffsetY;
					// reset the element range here although it will not be checked anymore as the report ends
					groupFooterPositionElementRange = null;
				}
				
				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				JRPrintBand printBand = summary.fill(columnFooterOffsetY - offsetY);

				if (summary.willOverflow() && summary.isSplitPrevented())
				{
					fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

					fillPageFooter(JRExpression.EVALUATION_DEFAULT);

					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);
					
					fillPageHeader(JRExpression.EVALUATION_DEFAULT);
					
					printBand = summary.refill(pageHeight - bottomMargin - offsetY - pageFooter.getHeight());

					fillBand(printBand);
					offsetY += printBand.getHeight();
				}
				else
				{
					//SummaryReport.17 test

					fillBand(printBand);
					offsetY += printBand.getHeight();

					if (
						!summary.willOverflow()
						&& offsetY <= lastPageColumnFooterOffsetY
						)
					{
						setLastPageFooter(true);
					}
					
					fillColumnFooters(JRExpression.EVALUATION_DEFAULT);
				}
				
				/*   */
				fillSummaryOverflow();

				//DONE
			}
			else
			{
				// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
				
				setLastPageFooter(true);

				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				//DONE
			}
		}
		else if (
				//columnIndex == 0 && 
				offsetY <= lastPageColumnFooterOffsetY)
		{
			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
				
				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);

				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);
				
				fillPageHeader(JRExpression.EVALUATION_DEFAULT);

				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY - pageFooter.getHeight());

				if (summary.willOverflow() && summary.isSplitPrevented() && isSubreport())
				{
					fillPageFooter(JRExpression.EVALUATION_DEFAULT);

					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);
					
					fillPageHeader(JRExpression.EVALUATION_DEFAULT);

					printBand = summary.refill(pageHeight - bottomMargin - offsetY - pageFooter.getHeight());
				}

				fillBand(printBand);
				offsetY += printBand.getHeight();

				/*   */
				fillSummaryOverflow();
				
				//DONE
			}
			else
			{
				// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
				
				setLastPageFooter(true);

				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				//DONE
			}
		}
		else
		{
			// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do
			
			fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

			fillPageFooter(JRExpression.EVALUATION_DEFAULT);

			resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
			resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
			resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
			scriptlet.callBeforePageInit();
			calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
			scriptlet.callAfterPageInit();

			addPage(false);

			fillPageHeader(JRExpression.EVALUATION_DEFAULT);

			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				//SummaryReport.18 test

				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY - pageFooter.getHeight());

				if (summary.willOverflow() && summary.isSplitPrevented() && isSubreport())
				{
					fillPageFooter(JRExpression.EVALUATION_DEFAULT);

					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);
					
					fillPageHeader(JRExpression.EVALUATION_DEFAULT);

					printBand = summary.refill(pageHeight - bottomMargin - offsetY - pageFooter.getHeight());
				}

				fillBand(printBand);
				offsetY += printBand.getHeight();
			}
			else
			{
				//SummaryReport.19 test
			}

			/*   */
			fillSummaryOverflow();
			
			//DONE
		}
	}


	/**
	 *
	 */
	private void fillSummaryWithLastFooterNoPageBands() throws JRException
	{
		if (
			!isSummaryNewPage
			//&& columnIndex == 0
			&& summary.getBreakHeight() <= lastPageColumnFooterOffsetY - offsetY
			)
		{
			setLastPageFooter(true);

			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				// deal with groupFooterPositionElementRange here because summary will attempt to use remaining space
				if (groupFooterPositionElementRange != null)
				{
					ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
					offsetY = columnFooterOffsetY;
					// reset the element range here although it will not be checked anymore as the report ends
					groupFooterPositionElementRange = null;
				}
				
				JRPrintBand printBand = summary.fill(columnFooterOffsetY - offsetY);

				if (summary.willOverflow() && summary.isSplitPrevented())
				{
					fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

					fillPageFooter(JRExpression.EVALUATION_DEFAULT);

					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);

					printBand = summary.refill(pageHeight - bottomMargin - offsetY);

					fillBand(printBand);
					offsetY += printBand.getHeight();
				}
				else
				{
					fillBand(printBand);
					offsetY += printBand.getHeight();

					fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

					fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				}

				/*   */
				fillSummaryOverflow();
				
				//DONE
			}
			else
			{
				// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do;

				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				//DONE
			}
		}
		else if (
			!isSummaryNewPage
			//&& columnIndex == 0
			&& summary.getBreakHeight() <= columnFooterOffsetY - offsetY
			)
		{
			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				// deal with groupFooterPositionElementRange here because summary will attempt to use remaining space
				if (groupFooterPositionElementRange != null)
				{
					ElementRangeUtil.moveContent(groupFooterPositionElementRange, columnFooterOffsetY);
					offsetY = columnFooterOffsetY;
					// reset the element range here although it will not be checked anymore as the report ends
					groupFooterPositionElementRange = null;
				}
				
				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				JRPrintBand printBand = summary.fill(columnFooterOffsetY - offsetY);

				if (summary.willOverflow() && summary.isSplitPrevented())
				{
					if (offsetY <= lastPageColumnFooterOffsetY)
					{
						setLastPageFooter(true);

						fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

						fillPageFooter(JRExpression.EVALUATION_DEFAULT);

						resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
						resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
						resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
						scriptlet.callBeforePageInit();
						calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
						scriptlet.callAfterPageInit();

						addPage(false);

						printBand = summary.refill(pageHeight - bottomMargin - offsetY);

						fillBand(printBand);
						offsetY += printBand.getHeight();
					}
					else
					{
						fillPageBreak(false, JRExpression.EVALUATION_DEFAULT, JRExpression.EVALUATION_DEFAULT, false);

						setLastPageFooter(true);

						printBand = summary.refill(lastPageColumnFooterOffsetY - offsetY);

						fillBand(printBand);
						offsetY += printBand.getHeight();

						fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

						fillPageFooter(JRExpression.EVALUATION_DEFAULT);
					}
				}
				else
				{
					fillBand(printBand);
					offsetY += printBand.getHeight();

					fillPageBreak(false, JRExpression.EVALUATION_DEFAULT, JRExpression.EVALUATION_DEFAULT, false);

					setLastPageFooter(true);

					if (summary.willOverflow())
					{
						printBand = summary.fill(lastPageColumnFooterOffsetY - offsetY);

						fillBand(printBand);
						offsetY += printBand.getHeight();
					}

					fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

					fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				}

				/*   */
				fillSummaryOverflow();
				
				//DONE
			}
			else
			{
				// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do;
				// it will be either the one in fillPageBreak or the following
				
				if (offsetY > lastPageColumnFooterOffsetY)
				{
					fillPageBreak(false, JRExpression.EVALUATION_DEFAULT, JRExpression.EVALUATION_DEFAULT, false);
				}

				setLastPageFooter(true);

				fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				//DONE
			}
		}
		else if (
				//columnIndex == 0 && 
				offsetY <= lastPageColumnFooterOffsetY)
		{
			// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do

			setLastPageFooter(true);

			fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

			fillPageFooter(JRExpression.EVALUATION_DEFAULT);

			summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

			if (summary != missingFillBand && summary.isToPrint())
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				summary.evaluate(JRExpression.EVALUATION_DEFAULT);

				JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY);

				if (summary.willOverflow() && summary.isSplitPrevented() && isSubreport())
				{
					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);

					printBand = summary.refill(pageHeight - bottomMargin - offsetY);
				}

				fillBand(printBand);
				offsetY += printBand.getHeight();

				/*   */
				fillSummaryOverflow();
			}
			
			//DONE
		}
		else
		{
			// do nothing about groupFooterPositionElementRange because the following fillColumnFooter will do;

			fillColumnFooters(JRExpression.EVALUATION_DEFAULT);

			fillPageFooter(JRExpression.EVALUATION_DEFAULT);

			resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
			resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
			resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
			scriptlet.callBeforePageInit();
			calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
			scriptlet.callAfterPageInit();

			addPage(false);

			fillPageHeader(JRExpression.EVALUATION_DEFAULT);

			//fillColumnHeader(JRExpression.EVALUATION_DEFAULT);

			setLastPageFooter(true);

			if (isSummaryNewPage)
			{
				fillPageFooter(JRExpression.EVALUATION_DEFAULT);

				summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

				if (summary != missingFillBand && summary.isToPrint())
				{
					resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
					resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
					resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
					scriptlet.callBeforePageInit();
					calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
					scriptlet.callAfterPageInit();

					addPage(false);

					summary.evaluate(JRExpression.EVALUATION_DEFAULT);

					JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY);

					if (summary.willOverflow() && summary.isSplitPrevented() && isSubreport())
					{
						resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
						resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
						resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
						scriptlet.callBeforePageInit();
						calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
						scriptlet.callAfterPageInit();

						addPage(false);

						printBand = summary.refill(pageHeight - bottomMargin - offsetY);
					}

					fillBand(printBand);
					offsetY += printBand.getHeight();

					/*   */
					fillSummaryOverflow();
				}
				
				//DONE
			}
			else
			{
				summary.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

				if (summary != missingFillBand && summary.isToPrint())
				{
					summary.evaluate(JRExpression.EVALUATION_DEFAULT);

					JRPrintBand printBand = summary.fill(columnFooterOffsetY - offsetY);

					if (summary.willOverflow() && summary.isSplitPrevented())//FIXMENOW check subreport here?
					{
						fillPageFooter(JRExpression.EVALUATION_DEFAULT);

						resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
						resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
						resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
						scriptlet.callBeforePageInit();
						calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
						scriptlet.callAfterPageInit();

						addPage(false);

						printBand = summary.refill(pageHeight - bottomMargin - offsetY);

						fillBand(printBand);
						offsetY += printBand.getHeight();
					}
					else
					{
						fillBand(printBand);
						offsetY += printBand.getHeight();

						fillPageFooter(JRExpression.EVALUATION_DEFAULT);
					}

					/*   */
					fillSummaryOverflow();
				}
				else
				{
					fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				}
				
				//DONE
			}
		}
	}


	/**
	 *
	 */
	private void fillSummaryOverflow() throws JRException
	{
		while (summary.willOverflow())
		{
			if (isSummaryWithPageHeaderAndFooter)
			{
				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
			}
			
			resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
			resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
			resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
			scriptlet.callBeforePageInit();
			calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
			scriptlet.callAfterPageInit();

			addPage(false);

			if (isSummaryWithPageHeaderAndFooter)
			{
				fillPageHeader(JRExpression.EVALUATION_DEFAULT);
			}
			
			JRPrintBand printBand = summary.fill(pageHeight - bottomMargin - offsetY - (isSummaryWithPageHeaderAndFooter?pageFooter.getHeight():0));

			fillBand(printBand);
			offsetY += printBand.getHeight();
		}

		resolveBandBoundElements(summary, JRExpression.EVALUATION_DEFAULT);

		if (isSummaryWithPageHeaderAndFooter)
		{
			if (offsetY > pageHeight - bottomMargin - lastPageFooter.getHeight())
			{
				fillPageFooter(JRExpression.EVALUATION_DEFAULT);
				
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, true);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				fillPageHeader(JRExpression.EVALUATION_DEFAULT);
			}
			
			if (lastPageFooter != missingFillBand)
			{
				setLastPageFooter(true);
			}
			
			fillPageFooter(JRExpression.EVALUATION_DEFAULT);
		}
	}


	/**
	 *
	 */
	private void fillBackground() throws JRException
	{
		if (log.isDebugEnabled() && !background.isEmpty())
		{
			log.debug("Fill " + fillerId + ": background at " + offsetY);
		}
		
		//offsetX = leftMargin;
		
		//if (!isSubreport)
		//{
		//  offsetY = pageHeight - pageFooter.getHeight() - bottomMargin;
		//}
		
		if (background.getHeight() <= pageHeight - bottomMargin - offsetY)
		{
			background.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);
			
			if (background.isToPrint())
			{
				background.evaluate(JRExpression.EVALUATION_DEFAULT);
				
				JRPrintBand printBand = background.fill(pageHeight - bottomMargin - offsetY);
				
				fillBand(printBand);
				//offsetY += printBand.getHeight();
			}
		}
	}


	/**
	 *
	 */
	private void addPage(boolean isResetPageNumber) throws JRException
	{
		if (isSubreport())
		{
			addPageToParent(false);
		}

		printPage = newPage();

		if (isResetPageNumber)
		{
			calculator.getPageNumber().setValue(Integer.valueOf(1));
		}
		else
		{
			calculator.getPageNumber().setValue(
				Integer.valueOf(((Number)calculator.getPageNumber().getValue()).intValue() + 1)
				);
		}

		calculator.getPageNumber().setOldValue(
			calculator.getPageNumber().getValue()
			);

		addPage(printPage);
		setFirstColumn();
		offsetY = topMargin;

		lastDetailOffsetX = -1;
		lastDetailOffsetY = -1;
		maxDetailOffsetY = 0;

		fillBackground();
	}

	/**
	 * Sets the column number value computed based on {@link #columnIndex columnIndex}
	 */
	private void setColumnNumberVariable()
	{
		JRFillVariable columnNumberVar = calculator.getColumnNumber();
		columnNumberVar.setValue(Integer.valueOf(columnIndex + 1));
		columnNumberVar.setOldValue(columnNumberVar.getValue());
	}

	/**
	 *
	 */
	private void fillPageBreak(
		boolean isResetPageNumber,
		byte evalPrevPage,
		byte evalNextPage,
		boolean isReprintGroupHeaders
		) throws JRException
	{
		if (isCreatingNewPage)
		{
			throw 
				new JRException(
					EXCEPTION_MESSAGE_KEY_INFINITE_LOOP_CREATING_NEW_PAGE,  
					(Object[])null 
					);
		}

		if (groups != null)
		{
			for (JRFillGroup group : groups)
			{
				if (group.getKeepTogetherElementRange() != null)
				{
					group.getKeepTogetherElementRange().expand(offsetY);
				}
			}
		}
		
		isCreatingNewPage = true;

		fillColumnFooters(evalPrevPage);

		fillPageFooter(evalPrevPage);

		resolveGroupBoundElements(evalPrevPage, false);
		resolveColumnBoundElements(evalPrevPage);
		resolvePageBoundElements(evalPrevPage);
		scriptlet.callBeforePageInit();
		calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
		scriptlet.callAfterPageInit();

		JRFillGroup keepTogetherGroup = null;
		
		if (groups != null)
		{
			for (JRFillGroup group : groups)
			{
				if (group.getKeepTogetherElementRange() != null)
				{
					keepTogetherGroup = group;
					break;
				}
			}
		}

		ElementRangeContents elementsToMove = null;
		
		if (keepTogetherGroup != null && keepTogetherGroup.getKeepTogetherElementRange() != null)
		{
			elementsToMove = ElementRangeUtil.removeContent(keepTogetherGroup.getKeepTogetherElementRange(),
					delayedActions);
		}

		addPage(isResetPageNumber);

		fillPageHeader(evalNextPage);

		fillColumnHeaders(evalNextPage);

		if (isReprintGroupHeaders)
		{
			fillGroupHeadersReprint(evalNextPage);
			
			ElementRange keepTogetherElementRange = keepTogetherGroup == null ? null : keepTogetherGroup.getKeepTogetherElementRange();

			if (
				keepTogetherElementRange != null
				&& offsetY > keepTogetherElementRange.getBottomY()
				)
			{
				throw new JRException("Keep together moved content does not fit on the new page.");
			}
		}

		moveKeepTogetherElementRangeContent(keepTogetherGroup, elementsToMove);

		isCreatingNewPage = false;
	}


	/**
	 *
	 */
	protected ElementRange fillColumnBand(JRFillBand band, byte evaluation) throws JRException
	{
		band.evaluate(evaluation);

		JRPrintBand printBand = band.fill(columnFooterOffsetY - offsetY);

		if (band.willOverflow())
		{
			boolean toRefill = band.isSplitPrevented();
			
			if (!toRefill)
			{
				if (groups != null)
				{
					// outer groups keep together is honored, while for the
					// inner keep together groups, the element range would be null 
					// in-between parent group breaks
					for (JRFillGroup group : groups)
					{
						if (group.getKeepTogetherElementRange() != null)
						{
							toRefill = true;
							break;
						}
					}
				}
			}
			
			if (toRefill)
			{
				fillPageBreak(false, evaluation, evaluation, true);

				printBand = band.refill(columnFooterOffsetY - offsetY);
			}
		}

		ElementRange elementRange = 
			new SimpleElementRange(
				getCurrentPage(), 
				columnIndex,
				offsetY
				);
		
		fillBand(printBand);
		offsetY += printBand.getHeight();
		
		elementRange.expand(offsetY);
		// we mark the element range here, because overflow content beyond this point
		// should be rendered normally, not moved in any way 

		while (band.willOverflow())
		{
			fillPageBreak(false, evaluation, evaluation, true);

			printBand = band.fill(columnFooterOffsetY - offsetY);

			fillBand(printBand);
			offsetY += printBand.getHeight();
		}

		resolveBandBoundElements(band, evaluation);
		
		return elementRange;
	}


	/**
	 *
	 */
	protected void fillFixedBand(JRFillBand band, byte evaluation) throws JRException
	{
		fillFixedBand(band, evaluation, true);
	}


	protected void fillFixedBand(JRFillBand band, byte evaluation, boolean allowShrinking) throws JRException
	{
		band.evaluate(evaluation);

		JRPrintBand printBand = band.fill();

		fillBand(printBand);
		offsetY += allowShrinking ? printBand.getHeight() : band.getHeight();

		resolveBandBoundElements(band, evaluation);
	}


	/**
	 *
	 */
	private void setNewPageColumnInBands()
	{
		title.setNewPageColumn(true);
		pageHeader.setNewPageColumn(true);
		columnHeader.setNewPageColumn(true);
		detailSection.setNewPageColumn(true);
		columnFooter.setNewPageColumn(true);
		pageFooter.setNewPageColumn(true);
		lastPageFooter.setNewPageColumn(true);
		summary.setNewPageColumn(true);
		noData.setNewPageColumn(true);

		if (groups != null && groups.length > 0)
		{
			for(int i = 0; i < groups.length; i++)
			{
				((JRFillSection)groups[i].getGroupHeaderSection()).setNewPageColumn(true);
				((JRFillSection)groups[i].getGroupFooterSection()).setNewPageColumn(true);
			}
		}
	}


	/**
	 *
	 */
	private void setNewGroupInBands(JRGroup group)
	{
		title.setNewGroup(group, true);
		pageHeader.setNewGroup(group, true);
		columnHeader.setNewGroup(group, true);
		detailSection.setNewGroup(group, true);
		columnFooter.setNewGroup(group, true);
		pageFooter.setNewGroup(group, true);
		lastPageFooter.setNewGroup(group, true);
		summary.setNewGroup(group, true);

		if (groups != null && groups.length > 0)
		{
			for(int i = 0; i < groups.length; i++)
			{
				((JRFillSection)groups[i].getGroupHeaderSection()).setNewGroup(group, true);
				((JRFillSection)groups[i].getGroupFooterSection()).setNewGroup(group, true);
			}
		}
	}


	/**
	 *
	 */
	private JRFillBand getCurrentPageFooter()
	{
		return isLastPageFooter ? lastPageFooter : pageFooter;
	}


	/**
	 *
	 */
	private void setLastPageFooter(boolean isLastPageFooter)
	{
		this.isLastPageFooter = isLastPageFooter;

		if (isLastPageFooter)
		{
			columnFooterOffsetY = lastPageColumnFooterOffsetY;
		}
	}

	/**
	 *
	 */
	private void fillNoData() throws JRException
	{
		if (log.isDebugEnabled() && !noData.isEmpty())
		{
			log.debug("Fill " + fillerId + ": noData at " + offsetY);
		}

		noData.evaluatePrintWhenExpression(JRExpression.EVALUATION_DEFAULT);

		if (noData.isToPrint())
		{
			while (noData.getBreakHeight() > pageHeight - bottomMargin - offsetY)
			{
				addPage(false);
			}

			noData.evaluate(JRExpression.EVALUATION_DEFAULT);

			JRPrintBand printBand = noData.fill(pageHeight - bottomMargin - offsetY);

			if (noData.willOverflow() && noData.isSplitPrevented() && isSubreport())
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				printBand = noData.refill(pageHeight - bottomMargin - offsetY);
			}

			fillBand(printBand);
			offsetY += printBand.getHeight();

			while (noData.willOverflow())
			{
				resolveGroupBoundElements(JRExpression.EVALUATION_DEFAULT, false);
				resolveColumnBoundElements(JRExpression.EVALUATION_DEFAULT);
				resolvePageBoundElements(JRExpression.EVALUATION_DEFAULT);
				scriptlet.callBeforePageInit();
				calculator.initializeVariables(ResetTypeEnum.PAGE, IncrementTypeEnum.PAGE);
				scriptlet.callAfterPageInit();

				addPage(false);

				printBand = noData.fill(pageHeight - bottomMargin - offsetY);

				fillBand(printBand);
				offsetY += printBand.getHeight();
			}
			resolveBandBoundElements(noData, JRExpression.EVALUATION_DEFAULT);
		}
	}

	
	/**
	 *
	 */
	private void setOffsetX()
	{
		if (columnDirection == RunDirectionEnum.RTL)
		{
			offsetX = pageWidth - rightMargin - columnWidth - columnIndex * (columnSpacing + columnWidth);
		}
		else
		{
			offsetX = leftMargin + columnIndex * (columnSpacing + columnWidth);
		}
	}

	
}
