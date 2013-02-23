package com.cm4j.test.guava.consist;

import java.util.concurrent.atomic.AtomicInteger;

import com.cm4j.test.guava.consist.entity.IEntity;

/**
 * 单个缓存值，与list相对应<br>
 * 例如数据库中的一行数据<br>
 * 注意：此类会被copy，所以不要包含大对象，如logger，以防止效率降低
 * 
 * @author Yang.hao
 * @since 2013-1-18 上午09:25:24
 * 
 */
public abstract class CacheEntry {

	/**
	 * 缓存状态 - 默认持久化
	 */
	private volatile DBState dbState = DBState.P;
	/**
	 * 在更新队列中的数量
	 */
	private final AtomicInteger numInUpdateQueue = new AtomicInteger(0);

	/**
	 * 此对象所依附的key
	 */
	private String attachedKey;

	AtomicInteger getNumInUpdateQueue() {
		return numInUpdateQueue;
	}

	/**
	 * 根据当前缓存对象解析hibernate entity进行数据保存
	 * 
	 * @return
	 */
	public abstract IEntity parseEntity();

	String getAttachedKey() {
		return attachedKey;
	}

	void setAttachedKey(String attachedKey) {
		this.attachedKey = attachedKey;
	}

	DBState getDbState() {
		return dbState;
	}

	synchronized void setDbState(DBState dbState) {
		this.dbState = dbState;
	}
}
