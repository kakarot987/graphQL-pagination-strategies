package com.portfolio.graphql.config;

import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.model.Product;
import com.portfolio.graphql.model.User;
import com.portfolio.graphql.repository.ActivityItemRepository;
import com.portfolio.graphql.repository.ProductRepository;
import com.portfolio.graphql.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ActivityItemRepository activityItemRepository;
    private final UserRepository userRepository;
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        log.info("Seeding demo data...");
        seedProducts();
        seedUsers();
        seedActivities();
        log.info("Data seeding complete: {} products, {} users, {} activities",
            productRepository.count(), userRepository.count(), activityItemRepository.count());
    }

    private void seedProducts() {
        String[][] products = {
            {"MacBook Pro 16\"", "Electronics", "Apple", "3499.99", "4.8", "Professional laptop with M3 Max chip, 48GB RAM"},
            {"Sony WH-1000XM5", "Electronics", "Sony", "349.99", "4.7", "Industry-leading noise canceling headphones"},
            {"Samsung 4K OLED TV 65\"", "Electronics", "Samsung", "1799.99", "4.6", "Stunning OLED display with 120Hz refresh rate"},
            {"Dyson V15 Detect", "Home & Garden", "Dyson", "749.99", "4.5", "Laser-guided vacuum with automatic suction adjustment"},
            {"Nike Air Max 270", "Footwear", "Nike", "149.99", "4.3", "Iconic Air Max cushioning for all-day comfort"},
            {"Levi's 501 Original Jeans", "Clothing", "Levi's", "79.99", "4.4", "The original straight fit, iconic since 1873"},
            {"Instant Pot Duo 7-in-1", "Kitchen", "Instant Pot", "89.99", "4.7", "Multi-use pressure cooker, slow cooker, rice cooker"},
            {"Kindle Paperwhite", "Electronics", "Amazon", "139.99", "4.6", "Waterproof with 3-month battery life"},
            {"Adidas Ultraboost 23", "Footwear", "Adidas", "189.99", "4.5", "Responsive running shoes with BOOST midsole"},
            {"Weber Spirit II E-310", "Home & Garden", "Weber", "549.99", "4.7", "3-burner gas grill with GS4 grilling system"},
            {"iPad Pro 12.9\"", "Electronics", "Apple", "1099.99", "4.8", "M2 chip with Liquid Retina XDR display"},
            {"Patagonia Down Jacket", "Clothing", "Patagonia", "229.99", "4.6", "800-fill power recycled down, DWR finish"},
            {"KitchenAid Stand Mixer", "Kitchen", "KitchenAid", "449.99", "4.9", "10-speed tilt-head mixer, 5-quart stainless bowl"},
            {"Canon EOS R6 Mark II", "Electronics", "Canon", "2499.99", "4.8", "40fps burst, in-body stabilization, 4K60 video"},
            {"Osprey Atmos AG 65L", "Sports", "Osprey", "299.99", "4.7", "Anti-Gravity suspension backpacking pack"},
            {"Bose SoundLink Revolve+", "Electronics", "Bose", "329.99", "4.5", "360-degree Bluetooth speaker, 17-hour battery"},
            {"Brooks Ghost 15", "Footwear", "Brooks", "139.99", "4.6", "Neutral daily trainer with DNA Loft cushioning"},
            {"Vitamix 5200 Blender", "Kitchen", "Vitamix", "449.99", "4.8", "Variable speed control, aircraft-grade blades"},
            {"North Face Venture 2", "Clothing", "The North Face", "99.99", "4.4", "Waterproof, breathable rain jacket, DryVent 2.5L"},
            {"Garmin Fenix 7 Pro", "Sports", "Garmin", "799.99", "4.7", "Multisport GPS watch with solar charging"},
            {"LEGO Technic Bugatti", "Toys", "LEGO", "449.99", "4.9", "3599-piece Bugatti Chiron detailed replica"},
            {"Theragun Pro Gen 5", "Sports", "Therabody", "599.99", "4.6", "Professional percussive therapy device"},
            {"Herman Miller Aeron", "Furniture", "Herman Miller", "1795.99", "4.8", "Ergonomic office chair, PostureFit SL support"},
            {"Roomba j9+", "Home & Garden", "iRobot", "699.99", "4.3", "Self-emptying robot vacuum with auto-refilling"},
            {"Hydro Flask 32oz", "Sports", "Hydro Flask", "49.99", "4.7", "TempShield insulation, 24hr cold / 12hr hot"},
        };

        List<Product> toSave = new ArrayList<>();
        for (int i = 0; i < products.length; i++) {
            String[] p = products[i];
            toSave.add(Product.builder()
                .name(p[0])
                .category(p[1])
                .brand(p[2])
                .price(Double.parseDouble(p[3]))
                .rating(Double.parseDouble(p[4]))
                .description(p[5])
                .stockCount(random.nextInt(200) + 1)
                .imageUrl("https://picsum.photos/seed/product" + i + "/400/400")
                .createdAt(LocalDateTime.now().minusDays(random.nextInt(365)))
                .build());
        }
        productRepository.saveAll(toSave);
    }

    private void seedUsers() {
        String[][] users = {
            {"alice_chen", "alice.chen@email.com", "Alice Chen", "ACTIVE", "CUSTOMER"},
            {"bob_martinez", "bob.m@email.com", "Bob Martinez", "ACTIVE", "CUSTOMER"},
            {"carol_johnson", "carol.j@email.com", "Carol Johnson", "ACTIVE", "PREMIUM"},
            {"david_kim", "d.kim@email.com", "David Kim", "INACTIVE", "CUSTOMER"},
            {"emma_wilson", "emma.w@email.com", "Emma Wilson", "ACTIVE", "PREMIUM"},
            {"frank_garcia", "frank.g@email.com", "Frank Garcia", "ACTIVE", "CUSTOMER"},
            {"grace_lee", "grace.l@email.com", "Grace Lee", "SUSPENDED", "CUSTOMER"},
            {"henry_taylor", "h.taylor@email.com", "Henry Taylor", "ACTIVE", "ADMIN"},
            {"isabelle_brown", "i.brown@email.com", "Isabelle Brown", "ACTIVE", "CUSTOMER"},
            {"jack_davis", "j.davis@email.com", "Jack Davis", "PENDING", "CUSTOMER"},
            {"kate_miller", "k.miller@email.com", "Kate Miller", "ACTIVE", "PREMIUM"},
            {"liam_anderson", "l.anderson@email.com", "Liam Anderson", "ACTIVE", "CUSTOMER"},
            {"maya_thompson", "m.thompson@email.com", "Maya Thompson", "ACTIVE", "CUSTOMER"},
            {"noah_white", "n.white@email.com", "Noah White", "INACTIVE", "CUSTOMER"},
            {"olivia_harris", "o.harris@email.com", "Olivia Harris", "ACTIVE", "PREMIUM"},
            {"peter_clark", "p.clark@email.com", "Peter Clark", "ACTIVE", "ADMIN"},
            {"quinn_lewis", "q.lewis@email.com", "Quinn Lewis", "ACTIVE", "CUSTOMER"},
            {"rachel_robinson", "r.robinson@email.com", "Rachel Robinson", "ACTIVE", "CUSTOMER"},
            {"sam_walker", "s.walker@email.com", "Sam Walker", "SUSPENDED", "CUSTOMER"},
            {"tina_hall", "t.hall@email.com", "Tina Hall", "ACTIVE", "PREMIUM"},
        };

        List<User> toSave = new ArrayList<>();
        for (int i = 0; i < users.length; i++) {
            String[] u = users[i];
            int orders = random.nextInt(50);
            toSave.add(User.builder()
                .username(u[0])
                .email(u[1])
                .fullName(u[2])
                .status(User.UserStatus.valueOf(u[3]))
                .role(u[4])
                .createdAt(LocalDateTime.now().minusDays(random.nextInt(730)))
                .lastLoginAt(LocalDateTime.now().minusHours(random.nextInt(720)))
                .orderCount(orders)
                .totalSpent(orders * (50 + random.nextDouble() * 450))
                .build());
        }
        userRepository.saveAll(toSave);
    }

    private void seedActivities() {
        String[] usernames = {"alice_chen", "bob_martinez", "carol_johnson", "emma_wilson",
                              "frank_garcia", "kate_miller", "maya_thompson", "olivia_harris"};
        String[][] actions = {
            {"PURCHASE", "Purchased", "MacBook Pro 16\"", "order-001"},
            {"REVIEW", "Left a 5-star review for", "Sony WH-1000XM5", "review-042"},
            {"WISHLIST", "Added to wishlist", "Garmin Fenix 7 Pro", "wish-013"},
            {"VIEW", "Viewed product page for", "iPad Pro 12.9\"", "prod-011"},
            {"SHARE", "Shared", "KitchenAid Stand Mixer", "share-007"},
            {"FOLLOW", "Started following", "Patagonia Brand", "brand-003"},
            {"PURCHASE", "Purchased", "Nike Air Max 270", "order-089"},
            {"REVIEW", "Left a 4-star review for", "Instant Pot Duo 7-in-1", "review-055"},
            {"WISHLIST", "Saved for later", "Herman Miller Aeron", "wish-029"},
            {"VIEW", "Browsed", "Canon EOS R6 Mark II", "prod-014"},
        };

        List<Activity> toSave = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String[] action = actions[i % actions.length];
            String username = usernames[i % usernames.length];
            long userId = (i % usernames.length) + 1L;

            toSave.add(Activity.builder()
                .userId(userId)
                .username(username)
                .activityType(Activity.ActivityType.valueOf(action[0]))
                .description(action[1] + " " + action[2])
                .targetId(action[3])
                .targetName(action[2])
                .timestamp(LocalDateTime.now().minusMinutes(random.nextInt(1440)))
                .metadata("{\"source\": \"web\", \"sessionId\": \"sess-" + (1000 + i) + "\"}")
                .build());
        }
        activityItemRepository.saveAll(toSave);
    }
}
