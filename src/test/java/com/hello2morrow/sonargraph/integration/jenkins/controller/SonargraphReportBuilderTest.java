package com.hello2morrow.sonargraph.integration.jenkins.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SonargraphReportBuilderTest {

	@Test
	public void testGetLanguages()
	{
		assertEquals("No language means all languages", "Java,CSharp,CPlusPlus", SonargraphReportBuilder.getLanguages(false, false, false));
		assertEquals("All languages", "Java,CSharp,CPlusPlus", SonargraphReportBuilder.getLanguages(true, true, true));
		assertEquals("Java and CSharp", "Java,CSharp", SonargraphReportBuilder.getLanguages(true, true, false));
		assertEquals("Java and CPlusPlus", "Java,CPlusPlus", SonargraphReportBuilder.getLanguages(true, false, true));
		assertEquals("CSharp and CPlusPlus", "CSharp,CPlusPlus", SonargraphReportBuilder.getLanguages(false, true, true));
		assertEquals("Java only", "Java", SonargraphReportBuilder.getLanguages(true, false, false));
		assertEquals("CSharp only", "CSharp", SonargraphReportBuilder.getLanguages(false, true, false));
		assertEquals("CPlusPlus only", "CPlusPlus", SonargraphReportBuilder.getLanguages(false, false, true));
	}
}
