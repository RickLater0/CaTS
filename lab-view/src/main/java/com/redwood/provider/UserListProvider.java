package com.redwood.provider;

import com.redwood.collections.java.UserJavaList;
import com.redwood.collections.kotlin.UserKotlinList;
import com.redwood.collections.scala.UserScalaList;
import com.redwood.collections_api.IUserList;


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
