package org.gridkit.nimble.scenario;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.statistics.StatsFactory;

public interface Scenario {
    String getName();
    
    <T> Play<T> play(Context<T> context);
    
    public interface Context<T> {
        String getContextId();
        
        StatsFactory<T> getStatsFactory();
        
        ExecutorService getExecutor();
        
        Collection<RemoteAgent> getAgents();
    }
}