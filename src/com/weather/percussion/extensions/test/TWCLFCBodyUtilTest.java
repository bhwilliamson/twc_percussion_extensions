package com.weather.percussion.extensions.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.weather.percussion.extensions.jexl.TWCLFCBodyUtil;

public class TWCLFCBodyUtilTest{
    
    //Test one node in a paragraph, base test
    private static final String TEST_INPUT = "<p><wxnode:module node=\"one\"/></p>";
    private static final String TEST_OUTPUT = "<wxnode:module node=\"one\"/>";    
    
    //Test two nodes in two paragraphs
    private static final String TEST_ONE_INPUT = "<p><wxnode:module node=\"one\"/></p><p><wxnode:module node=\"two\"/></p>";
    private static final String TEST_ONE_OUTPUT = "<wxnode:module node=\"one\"/><wxnode:module node=\"two\"/>";
    
    //Test node with text on either side, with <hr/>
    private static final String TEST_TWO_INPUT = "<p>This is text before the node. <wxnode:module node=\"one\"/> And this is text after the node.<hr/>This is the second paragraph.</p>";
    private static final String TEST_TWO_OUTPUT = "<wxnode:module node=\"one\"/><p>This is text before the node.  And this is text after the node.<hr>This is the second paragraph.</p>";

    //Test nested strong within p all wrapping node
    private static final String TEST_THREE_INPUT = "<p>This is text before the <strong>node. <wxnode:module node=\"one\"/></strong> And this is text after the node</p>";
    private static final String TEST_THREE_OUTPUT = "<wxnode:module node=\"one\"/><p>This is text before the <strong>node. </strong> And this is text after the node</p>";    
    
    //Test node at top level (not within p tag for some reason)
    private static final String TEST_FOUR_INPUT = "<wxnode:module node=\"one\"/><p>This is text before the node.</p>";
    private static final String TEST_FOUR_OUTPUT = "<wxnode:module node=\"one\"/><p>This is text before the node.</p>";
    
    //Test two nodes within same paragraph, keep empty p tags if no nodes removed from them
    private static final String TEST_FIVE_INPUT = "<p>Here is the first node: <wxnode:module node=\"one\"/> And here is the second: <wxnode:module node=\"two\"/></p><p></p>";
    private static final String TEST_FIVE_OUTPUT = "<wxnode:module node=\"one\"/><wxnode:module node=\"two\"/><p>Here is the first node:  And here is the second: </p><p></p>";
    
    //Bulk test
    private static final String TEST_SIX_INPUT = "<p>brian test 07 her eis some <strong>text and I want</strong> to do some formatting of it.</p><p>How does this format?</p><p><wxnode:module align=\"left\" alt=\"\" articleurl=\"\" assetid=\"\" captionoverride=\"\" class=\"singleimage\" clickable=\"no\" clicksizecode=\"\" credit=\"\" links=\"\" showcaption=\"no\" sizecode=\"10\" synopsis=\"\" title=\"\" type=\"internalasset\"/></p><p>before the node<wxnode:module align=\"left\" alt=\"\" articleurl=\"\" caption=\"no\" class=\"singleimage\" clickurl=\"\" credit=\"\" height=\"\" links=\"\" src=\"\" synopsis=\"\" title=\"\" type=\"externalasset\" width=\"\"/> after the node</p><p><wxnode:module class=\"slideshow\" collectionid=\"\" sizecode=\"\" title=\"\" /></p><p><wxnode:module class=\"staticmap\" clicksizecode=\"\" helptext=\"\" links=\"\" mapid=\"\" sizecode=\"\" title=\"\"/><wxnode:module autoplay=\"false\" bcplayerid=\"1543561898001\" bcplayerkey=\"AQ~~,AAAAAAQxtuk~,N9g8AOtC12eobrWkZvrqKiXxOtGg-8h1\" class=\"corsican-video\" companionads=\"\" forceautoplay=\"false\" gridsize=\"4\" inpage=\"true\" position=\"left\" primary=\"false\" showdescription=\"false\" showheadline=\"true\" showplaylist=\"true\" showrelatedlinks=\"false\" videos=\"class=video;q=clip:360,clip:361,clip:362,clip:363|class=video;q=coll:169\"/></p><p><wxnode:module allowtransparency=\"true\" class=\"coveritlive\" frameborder=\"0\" height=\"750px\" href=\"\" scrolling=\"no\" src=\"\" title=\"\" width=\"600px\"/></p>";
    private static final String TEST_SIX_OUTPUT = "<p>brian test 07 her eis some <strong>text and I want</strong> to do some formatting of it.</p><p>How does this format?</p><wxnode:module align=\"left\" alt=\"\" articleurl=\"\" assetid=\"\" captionoverride=\"\" class=\"singleimage\" clickable=\"no\" clicksizecode=\"\" credit=\"\" links=\"\" showcaption=\"no\" sizecode=\"10\" synopsis=\"\" title=\"\" type=\"internalasset\"/><wxnode:module align=\"left\" alt=\"\" articleurl=\"\" caption=\"no\" class=\"singleimage\" clickurl=\"\" credit=\"\" height=\"\" links=\"\" src=\"\" synopsis=\"\" title=\"\" type=\"externalasset\" width=\"\"/><p>before the node after the node</p><wxnode:module class=\"slideshow\" collectionid=\"\" sizecode=\"\" title=\"\"/><wxnode:module class=\"staticmap\" clicksizecode=\"\" helptext=\"\" links=\"\" mapid=\"\" sizecode=\"\" title=\"\"/><wxnode:module autoplay=\"false\" bcplayerid=\"1543561898001\" bcplayerkey=\"AQ~~,AAAAAAQxtuk~,N9g8AOtC12eobrWkZvrqKiXxOtGg-8h1\" class=\"corsican-video\" companionads=\"\" forceautoplay=\"false\" gridsize=\"4\" inpage=\"true\" position=\"left\" primary=\"false\" showdescription=\"false\" showheadline=\"true\" showplaylist=\"true\" showrelatedlinks=\"false\" videos=\"class=video;q=clip:360,clip:361,clip:362,clip:363|class=video;q=coll:169\"/><wxnode:module allowtransparency=\"true\" class=\"coveritlive\" frameborder=\"0\" height=\"750px\" href=\"\" scrolling=\"no\" src=\"\" title=\"\" width=\"600px\"/>";
    
    //Test two nodes within same paragraph, keep empty p tags if no nodes removed from them
    private static final String TEST_SEVEN_INPUT = "<div><p>Here is the first node: <wxnode:module node=\"one\"/></p><p>And here is the second: <wxnode:module node=\"two\"/></p></div>";
    private static final String TEST_SEVEN_OUTPUT = "<wxnode:module node=\"one\"/><wxnode:module node=\"two\"/><div><p>Here is the first node: </p><p>And here is the second: </p></div>";
    
    //Basic table test
    private static final String TABLE_TEST_ONE_INPUT = "<p>Here is the first paragraph</p><table align=\"right\">foo</table><p>Here is the second paragraph</p>";
    private static final String TABLE_TEST_ONE_OUTPUT = "<p>Here is the first paragraph</p><table align=\"right\" class=\"wx-right\">foo</table><p>Here is the second paragraph</p>";
    private static final String TABLE_TEST_TWO_INPUT = "<table align=\"left\">foo</table><p>Here is the second paragraph</p>";
    private static final String TABLE_TEST_TWO_OUTPUT = "<table align=\"left\" class=\"wx-left\">foo</table><p>Here is the second paragraph</p>";
    private static final String TABLE_TEST_THREE_INPUT = "<table cellpadding=\"0\"></table><table>foo</table>";
    private static final String TABLE_TEST_THREE_OUTPUT = "<table cellpadding=\"0\"></table><table>foo</table>";    
    
    @Test
    public void test_moveWxNodesOutsideMarkup() {
        TWCLFCBodyUtil twcWxNodeUtil = new TWCLFCBodyUtil();
        
        assertTrue(TEST_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_INPUT)));
        assertTrue(TEST_ONE_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_ONE_INPUT)));
        assertTrue(TEST_TWO_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_TWO_INPUT)));
        assertTrue(TEST_THREE_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_THREE_INPUT)));
        assertTrue(TEST_FOUR_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_FOUR_INPUT)));
        assertTrue(TEST_FIVE_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_FIVE_INPUT)));
        assertTrue(TEST_SIX_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_SIX_INPUT)));
        assertTrue(TEST_SEVEN_OUTPUT.equals(twcWxNodeUtil.processBody(TEST_SEVEN_INPUT)));
        assertTrue(TABLE_TEST_ONE_OUTPUT.equals(twcWxNodeUtil.processBody(TABLE_TEST_ONE_OUTPUT)));
        assertTrue(TABLE_TEST_TWO_OUTPUT.equals(twcWxNodeUtil.processBody(TABLE_TEST_TWO_OUTPUT)));
        assertTrue(TABLE_TEST_THREE_OUTPUT.equals(twcWxNodeUtil.processBody(TABLE_TEST_THREE_OUTPUT)));
    }

}