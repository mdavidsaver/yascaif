package yascaif.cli;

import java.util.List;

import yascaif.CA;

public interface Command {
	public void process(CA ca, List<String> PVs);
}
