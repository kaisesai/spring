package example.scannable;

import org.springframework.stereotype.Service;

@Service
public class LiukaiServiceImpl implements LiukaiService {

	@Override
	public String sayHello(String name) {
		String result = "hello " + name + "!";
		System.out.println(result);
		return result;
	}

}
