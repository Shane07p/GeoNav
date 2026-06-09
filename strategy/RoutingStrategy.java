package strategy;

import graph.Graph;
import model.Node;
import model.Route;

/**
 * Strategy Pattern interface for routing algorithms.
 * Allows swapping between different pathfinding algorithms at runtime.
 */

public interface RoutingStrategy {

    Route findRoute(Graph graph, Node src, Node dest);

    String getStrategyName();
}
