package com.redwood.collections;

public class UserListProvider {

	public enum ImplType { JAVA, SCALA, KOTLIN }

	@SuppressWarnings("unchecked")
	public static <T> IUserList<T> createList(ImplType type) {
		return switch (type) {
			case JAVA -> new UserJavaList<>();
			case SCALA -> (IUserList<T>) new UserScalaList<>();
			case KOTLIN -> new UserKotlinList<>();
		};
	}
}
