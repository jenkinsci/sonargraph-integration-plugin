/*******************************************************************************
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2016 hello2morrow GmbH
 * mailto: info AT hello2morrow DOT com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *******************************************************************************/
package com.hello2morrow.sonargraph.integration.jenkins.foundation;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SonargraphLogger
{
    public static final Logger INSTANCE = Logger.getLogger("com.hello2morrow.sonargraph.integration.jenkins");

    public static void logToConsoleOutput(final PrintStream logger, final Level level, final String message, final Exception ex)
    {
        assert logger != null : "Parameter 'logger' of method 'logToConsoleOutput' must not be null";
        logger.print("[" + level.toString() + "] <SONARGRAPH> " + message);
        if(ex != null)
        {
        	logger.print(" " + ex.getMessage());
        }
        logger.println();
        INSTANCE.log(level, message);
    }
    
    private SonargraphLogger()
    {
    	
    }
}
