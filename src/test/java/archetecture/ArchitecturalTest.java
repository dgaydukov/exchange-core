package archetecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderchecks.PostOrderCheck;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(packages = "com.exchange.core")
public class ArchitecturalTest {

  private final JavaClasses importedClasses = new ClassFileImporter().importPackages(
      "com.exchange.core");

  @Test
  public void orderBookImplementationsOnlyInOrderBookPackageTest() {
    ArchRule orderBookRule = classes()
        .that().implement(OrderBook.class)
        .should().resideInAnyPackage("..matching.orderbook..");
    orderBookRule.check(importedClasses);
  }

  @Test
  public void orderBookClassesImplementOrderBookInterfaceTest() {
    ArchRule orderBookRule = classes()
        .that().implement(OrderBook.class)
        .should().haveNameMatching(".*OrderBook");
    orderBookRule.check(importedClasses);
  }

  @Test
  public void validateOrderChecksInsideOrderBook() {
    ArchRule notImplementPreOrderCheck = classes()
        .that().implement(OrderBook.class)
        .should().onlyHaveDependentClassesThat().doNotImplement(PreOrderCheck.class);
    ArchRule notImplementPostOrderCheck = classes()
        .that().implement(OrderBook.class)
        .should().onlyHaveDependentClassesThat().doNotImplement(PostOrderCheck.class);
    notImplementPreOrderCheck.check(importedClasses);
    notImplementPostOrderCheck.check(importedClasses);
  }
}