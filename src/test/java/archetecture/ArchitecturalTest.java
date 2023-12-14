package archetecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderchecks.PostOrderCheck;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(packages = "com.exchange.core", importOptions = {DoNotIncludeTests.class})
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
    ArchRule archRule = classes()
        .that().implement(OrderBook.class)
        .should(new ArchCondition<>("yup") {
          @Override
          public void check(JavaClass javaClass, ConditionEvents events) {
            javaClass.getDirectDependenciesFromSelf()
                .forEach(d -> {
                  JavaClass target = d.getTargetClass();
                  if (target.getName().contains("PreOrderCheck") || target.getName().contains("PostOrderCheck")){
                    events.add(new SimpleConditionEvent(javaClass, false, d.getOriginClass().getName()+" shouldn't implement PreOrderCheck/PostOrderCheck"));
                  }
                });
          }
        });
    archRule.check(importedClasses);
  }
}