import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MultiWebCrawler {
    private final Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();
    private final String saveFile = "crawler_state.txt";
    private final int maxDepth;
    private final String domain;
    private volatile boolean isRunning = true;
    private final ExecutorService executorService;

    public MultiWebCrawler(String startUrl, int numberOfThreads, int maxDepth) {
        this.maxDepth = maxDepth;
        this.domain = getDomain(startUrl);
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
        loadState();
        if (visitedUrls.isEmpty() && startUrl != null && !startUrl.isEmpty()) {
            urlQueue.offer(startUrl);
        }
        if (urlQueue.isEmpty() && !visitedUrls.contains(startUrl)) {
            urlQueue.offer(startUrl); // Add starting URL if not visited
        }
    }

    private String getDomain(String url) {
        try {
            URL urlObj = URI.create(url).toURL();
            return urlObj.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    public void start() {
    System.out.println("Starting crawler with " +
            ((ThreadPoolExecutor) executorService).getCorePoolSize() + " threads");

    // Submit crawler workers to the executor service
    for (int i = 0; i < ((ThreadPoolExecutor) executorService).getCorePoolSize(); i++) {
        executorService.submit(new CrawlerWorker());
    }

    // Add a shutdown hook to save the state and stop gracefully
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("\nShutting down crawler...");
        shutdown();
    }));

    // Monitor progress in a non-blocking way
    while (isRunning) {
        try {
            System.out.println("Queue size: " + urlQueue.size() + ", Visited: " + visitedUrls.size());
            Thread.sleep(5000); // Print status every 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


    private class CrawlerWorker implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    String url = urlQueue.poll(1, TimeUnit.SECONDS);
                    if (url == null) continue;

                    synchronized (visitedUrls) {
                        if (visitedUrls.contains(url)) {
                            System.out.println(Thread.currentThread().getName() + " - URL already visited: " + url);
                        } else {
                            crawl(url, 0);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }


        private void crawl(String url, int depth) {
            if (depth > maxDepth || !isRunning) return;
        
            visitedUrls.add(url); // Mark URL as visited
            saveState();          // Save state to file
        
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(5000)
                        .get();
        
                System.out.println(Thread.currentThread().getName() + " - Crawling: " + url);
                processPage(doc);
        
                // Extract and enqueue new URLs
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String nextUrl = link.absUrl("href");
                    if (isValidUrl(nextUrl)) {
                        // System.out.println("Enqueuing URL: " + nextUrl);
                        urlQueue.offer(nextUrl); // Enqueue new URL
                    }
                }
            } catch (Exception e) {
                System.err.println("Error crawling " + url + ": " + e.getMessage());
            }
        }
        

        private void processPage(Document doc) {
            String title = doc.title();
            System.out.println("Title: " + title);
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && !url.isEmpty() && url.startsWith("http")
                && getDomain(url).endsWith(domain) // Match subdomains
                && !visitedUrls.contains(url);
    }
    

    private synchronized void saveState() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(saveFile),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String url : visitedUrls) {
                writer.write(url);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving crawler state: " + e.getMessage());
        }
    }

    private synchronized void loadState() {
        try {
            if (Files.exists(Paths.get(saveFile))) {
                List<String> urls = Files.readAllLines(Paths.get(saveFile));
                visitedUrls.addAll(urls);
                System.out.println("Loaded " + visitedUrls.size() + " URLs from previous session");
    
                // Only add URLs to the queue if they haven't been visited
                for (String url : urls) {
                    if (!visitedUrls.contains(url)) {
                        urlQueue.offer(url);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading crawler state: " + e.getMessage());
        }
    }
    

    public void shutdown() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        saveState();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the starting URL: ");
            String startUrl = scanner.nextLine();

            System.out.print("Enter the number of threads (1-10): ");
            int threads = Math.min(10, Math.max(1, scanner.nextInt()));

            System.out.print("Enter the maximum crawl depth: ");
            int maxDepth = scanner.nextInt();

            MultiWebCrawler crawler = new MultiWebCrawler(startUrl, threads, maxDepth);
            crawler.start();
        }
    }
}
