filewatcher
===========

Simple WatchService wrapper for Java 8

## Basic javadoc
```java
class io.ruck.filewatcher.Watcher
   static void	register(Path base, WatchListener<Path> listener, WatchEvent.Kind<Path>... events) 
   static void	unregister(Path base, WatchListener<Path> listener) 
   
interface io.ruck.filewatcher.WatchListener<T>
   void handleEvent(Path path, WatchEvent.Kind kind)
```

## Implementation

Essentially, filewatcher spawns a single thread to follow a (WatchService)[https://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchService.html].  This Thread sleeps until a WatchKey is returned, and then notifies the appropriate WatchListeners.

