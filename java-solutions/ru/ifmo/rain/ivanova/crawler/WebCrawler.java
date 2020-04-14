package ru.ifmo.rain.ivanova.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author sasha.pff
 * <p>
 * Implementation of Crawler for websites.
 */
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;

    /**
     * Constructor to initialize with {@code Downloader}, number of downloading pages,
     * number of processing pages and number of downloading pages per one host.
     *
     * @param downloader  {@code Downloader} to use.
     * @param downloaders number of downloading pages.
     * @param extractors  number of processing pages.
     * @param perHost     number of downloading pages per one host.
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private class HostDownloader {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private int readyToRun = perHost;

        synchronized private void finish() {
            readyToRun++;
        }

        synchronized private void ready() {
            if (readyToRun > 0) {
                run();
            }
        }

        synchronized private void run() {
            Runnable task = tasks.poll();
            if (task != null) {
                readyToRun--;
                downloaders.submit(() -> {
                    try {
                        task.run();
                    } catch (Exception ignored) {
                    } finally {
                        finish();
                        ready();
                    }
                });
            }
        }

        synchronized void submit(Runnable task) {
            tasks.add(task);
            ready();
        }

    }

    private class WebDownloader {
        private final List<String> downloaded = Collections.synchronizedList(new ArrayList<>());
        private final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        private final Set<String> addedUrls = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<String, HostDownloader> addedHosts = new ConcurrentHashMap<>();
        private final int depth;
        private Phaser phaser;
        private ConcurrentLinkedQueue<String> queueToTake = new ConcurrentLinkedQueue<>();
        private ConcurrentLinkedQueue<String> queueToAdd = new ConcurrentLinkedQueue<>();

        private String getHost(String url) {
            try {
                return URLUtils.getHost(url);
            } catch (MalformedURLException e) {
                errors.put(url, e);
                return null;
            }
        }

        private void download(String url, final int d) {
            String host = getHost(url);
            if (host != null) {
                HostDownloader hostDownloader;
                if ((hostDownloader = addedHosts.get(host)) == null) {
                    addedHosts.put(host, hostDownloader = new HostDownloader());
                }
                phaser.register();
                hostDownloader.submit(() -> {
                    try {
                        final Document document = downloader.download(url);
                        if (depth - d > 1) {
                            phaser.register();
                            extractors.submit(() -> {
                                try {
                                    queueToAdd.addAll(document.extractLinks());
                                } catch (IOException e) {
                                    errors.put(url, e);
                                } finally {
                                    phaser.arrive();
                                }
                            });
                        }
                        downloaded.add(url);
                    } catch (IOException e) {
                        errors.put(url, e);
                    } finally {
                        phaser.arrive();
                    }
                });
            }
        }

        void swapQueues() {
            ConcurrentLinkedQueue<String> tmp = queueToAdd;
            queueToAdd = queueToTake;
            queueToTake = tmp;
        }

        void bfs(int d) {
            if (d == depth) {
                return;
            }
            swapQueues();
            phaser = new Phaser();
            phaser.register();
            while (!queueToTake.isEmpty()) {
                String url = queueToTake.poll();
                if (addedUrls.add(url)) {
                    download(url, d);
                }
            }
            phaser.arriveAndAwaitAdvance();
            bfs(d + 1);
        }

        WebDownloader(int depth) {
            this.depth = depth;
        }

        void run(String url) {
            queueToAdd.add(url);
            bfs(0);
        }

        Result result() {
            return new Result(downloaded, errors);
        }

    }

    @Override
    public Result download(String url, int depth) {
        WebDownloader webDownloader = new WebDownloader(depth);
        webDownloader.run(url);
        return webDownloader.result();
    }

    @Override
    public void close() {
        extractors.shutdown();
        downloaders.shutdown();
        try {
            extractors.awaitTermination(0, TimeUnit.SECONDS);
            downloaders.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Can't terminate");
        }
    }

    private static int parseArgument(String[] args, int index) {
        return args.length <= index ? 1 : Integer.parseInt(args[index]);
    }

    /**
     * Main function to run using command line.
     *
     * @param args arguments to create {@code WebCrawler}.
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Incorrect arguments");
            return;
        }
        int depth = parseArgument(args, 1);
        try (Crawler crawler = new WebCrawler(new CachingDownloader(), depth,
                parseArgument(args, 2), parseArgument(args, 3))) {
            crawler.download(args[0], depth);
        } catch (IOException e) {
            System.err.println("Can't initialize CachingDownloader");
        }
    }
}