package cl.duoc.lab.bdd;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runner principal de Cucumber sobre JUnit 5.
 *
 * - Descubre los .feature ubicados en src/test/resources/features
 * - El glue (step definitions y hooks) se toma del package de esta clase:
 *   cl.duoc.lab.bdd.* (incluye sub-packages steps/ y hooks/)
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class RunCucumberTest {
}
