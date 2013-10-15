package com.cm4j.test.guava.consist;

import com.cm4j.dao.hibernate.HibernateDao;
import com.cm4j.test.guava.service.ServiceManager;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Reference的一些公共方法
 *
 * @author Yang.hao
 * @since 2013-3-2 上午10:42:59
 */
public abstract class AbsReference {

    // 用于存放暂时未被删除对象
    // 里面对象只能被删除，不可更改状态
    private final Set<CacheEntry> deletedSet = new HashSet<CacheEntry>();

    /**
     * 此对象所依附的key
     */
    private String attachedKey;

    /**
     * 获取值
     *
     * @param <V>
     * @return
     */
    public abstract <V> V get();

    /**
     * 更新entry<br>
     * 因为子类有范型，类型兼容问题，所以子类多写一个update()方法来规定对象给外部调用，而此方法也转类型调用update()
     *
     * @param e
     */
    protected abstract void updateEntry(CacheEntry e);

    /**
     * 删除entry<br>
     * 因为子类有范型，类型兼容问题，所以子类多写一个delete()方法来规定对象给外部调用，而此方法也转类型调用update()
     *
     * @param e
     */
    protected abstract void deleteEntry(CacheEntry e);

    /**
     * 是否所有对象都与数据库保持一致(状态P)
     * 缓存过期是否可移除的判断条件之一，此方法在lock下被调用<br>
     *
     * @return
     */
    protected boolean isAllPersist() {
        if (getDeletedSet().size() > 0) {
            return false;
        }
        return allPersist();
    }

    /**
     * 非deleteSet的数据是否都是P的状态
     * 缓存过期是否可移除的判断条件之一，此方法在lock下被调用
     *
     * @return
     */
    protected abstract boolean allPersist();

    /**
     * 持久化到数据库，用于persistAndRemove()，此方法在lock下被调用
     */
    protected abstract void persistDB();

    /**
     * 缓存中单个对象的修改后更改此对象的状态，此方法在lock下被调用
     *
     * @param entry
     * @param dbState
     * @return
     */
    protected abstract boolean changeDbState(CacheEntry entry, DBState dbState);

    /**
     * 在从db获取数据之后，设置缓存内数据所属的key，用来辨识此对象是哪个缓存的
     *
     * @param attachedKey
     */
    protected abstract void attachedKey(String attachedKey);

    /**
     * 持久化但不移除
     */
    public void persist() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), false);
    }

    /**
     * 持久化后移除
     */
    public void persistAndRemove() {
        ConcurrentCache.getInstance().persistAndRemove(getAttachedKey(), true);
    }

    /**
     * 将deleteSet中的对象持久化
     */
    protected void persistDeleteSet() {
        HibernateDao hibernate = ServiceManager.getInstance().getSpringBean("hibernateDao");
        // deleteSet数据处理
        for (CacheEntry v : getDeletedSet()) {
            hibernate.delete(v);
            changeDbState(v, DBState.P);
            // 占位：发送到更新队列，状态P
            ConcurrentCache.getInstance().sendToUpdateQueue(v);
        }
        getDeletedSet().clear();
    }

    /**
     * 检查deleteSet，如果存在则修改状态
     *
     * @param entry
     * @param dbState
     *         如果存在于deleteSet中，则状态必须为P，因为deleteSet中对象不能被修改，只能是在删除后被修改为P
     * @return 是否存在
     */
    protected boolean checkAndDealDeleteSet(CacheEntry entry, DBState dbState) {
        // deleteSet数据处理
        Iterator<CacheEntry> itor = getDeletedSet().iterator();
        while (itor.hasNext()) {
            CacheEntry v = itor.next();
            // 进入deleteSet的对象只能被写入，
            if (v == entry) {
                Preconditions.checkArgument(DBState.P == dbState, "对象被删除后不允许再修改");
                itor.remove();
                return true;
            }
        }
        return false;
    }

    protected String getAttachedKey() {
        return attachedKey;
    }

    protected void setAttachedKey(String attachedKey) {
        this.attachedKey = attachedKey;
        attachedKey(attachedKey);
    }

    public Set<CacheEntry> getDeletedSet() {
        return deletedSet;
    }
}
