package example.scannable;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 打印日志的切面
 */
@Component
@Aspect
public class LiukaiLogAspect {

	@Pointcut("execution(* example.scannable.LiukaiService.*(..))")
	public void pointCut() {
	}

	/**
	 * 方法前置通知
	 *
	 * @param joinPoint
	 */
	@Before(value = "pointCut()")
	public void methodBefore(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println(
				"执行目标方法【" + methodName + "】之前执行<前置通知>,入参" + Arrays.asList(joinPoint.getArgs()));
	}

	/**
	 * 方法后置通知
	 *
	 * @param joinPoint
	 */
	@After(value = "pointCut()")
	public void methodAfter(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println(
				"执行目标方法【" + methodName + "】之前执行<后置通知>,入参" + Arrays.asList(joinPoint.getArgs()));
	}

	/**
	 * 方法返回通知
	 *
	 * @param joinPoint
	 */
	@AfterReturning(value = "pointCut()")
	public void methodReturning(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println(
				"执行目标方法【" + methodName + "】之前执行<返回通知>,入参" + Arrays.asList(joinPoint.getArgs()));
	}

	/**
	 * 方法异常通知
	 *
	 * @param joinPoint
	 */
	@AfterThrowing(value = "pointCut()")
	public void methodAfterThrowing(JoinPoint joinPoint) {
		String methodName = joinPoint.getSignature().getName();
		System.out.println(
				"执行目标方法【" + methodName + "】之前执行<异常通知>,入参" + Arrays.asList(joinPoint.getArgs()));

	}

}
