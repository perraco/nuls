/**
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.consensus.service.impl;

import io.nuls.consensus.cache.manager.block.BlockCacheManager;
import io.nuls.consensus.utils.ConsensusTool;
import io.nuls.core.chain.entity.Block;
import io.nuls.core.chain.entity.BlockHeader;
import io.nuls.core.chain.entity.NulsDigestData;
import io.nuls.core.chain.entity.Transaction;
import io.nuls.core.context.NulsContext;
import io.nuls.core.dto.Page;
import io.nuls.core.exception.NulsException;
import io.nuls.core.utils.log.Log;
import io.nuls.db.dao.BlockHeaderService;
import io.nuls.db.entity.BlockHeaderPo;
import io.nuls.ledger.service.intf.LedgerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 * @date 2018/1/10
 */
public class BlockStorageService {
    private static final BlockStorageService INSTANCE = new BlockStorageService();

    private BlockHeaderService headerDao = NulsContext.getServiceBean(BlockHeaderService.class);
    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private BlockCacheManager blockCacheManager = BlockCacheManager.getInstance();

    private BlockStorageService() {
    }

    public static BlockStorageService getInstance() {
        return INSTANCE;
    }

    public Block getBlock(long height) throws Exception {
        Block block = blockCacheManager.getBlock(height);
        if (null != block) {
            return block;
        }
        BlockHeader header = getBlockHeader(height);
        if (null == header) {
            return null;
        }
        List<Transaction> txList = null;
        try {
            txList = ledgerService.getTxList(height);
        } catch (Exception e) {
            Log.error(e);
        }
        return fillBlock(header, txList);
    }

    public Block getBlock(String hash) throws Exception {
        Block block = blockCacheManager.getBlock(hash);
        if (null != block) {
            return block;
        }
        BlockHeader header = getBlockHeader(hash);
        if (null == header) {
            return null;
        }
        List<Transaction> txList = null;
        try {
            txList = ledgerService.getTxList(header.getHeight());
        } catch (Exception e) {
            Log.error(e);
        }
        return fillBlock(header, txList);
    }

    private Block fillBlock(BlockHeader header, List<Transaction> txList) {
        Block block = new Block();
        block.setTxs(txList);
        block.setHeader(header);
        return block;
    }


    public List<Block> getBlockList(long startHeight, long endHeight) throws NulsException {
        List<Block> blockList = new ArrayList<>();
        List<BlockHeaderPo> poList = headerDao.getHeaderList(startHeight, endHeight);
        List<Long> heightList = new ArrayList<>();
        if (!poList.isEmpty()) {
            List<Transaction> txList = null;
            try {
                txList = ledgerService.getTxList(startHeight, endHeight);
            } catch (Exception e) {
                Log.error(e);
            }
            Map<Long, List<Transaction>> txListGroup = txListGrouping(txList);
            for (BlockHeaderPo po : poList) {
                BlockHeader header = null;
                try {
                    header = ConsensusTool.fromPojo(po);
                } catch (NulsException e) {
                    throw e;
                }
                heightList.add(header.getHeight());
                blockList.add(fillBlock(header, txListGroup.get(header.getHeight())));
            }
        }
        if ((endHeight - startHeight + 1) > blockList.size()) {
            for (long i = startHeight; i <= endHeight; i++) {
                if (heightList.contains(i)) {
                    continue;
                }
                try {
                    blockList.add(this.getBlock(i));
                } catch (Exception e) {
                    Log.error(e);
                }
            }
        }
        return blockList;
    }

    private Map<Long, List<Transaction>> txListGrouping(List<Transaction> txList) {
        Map<Long, List<Transaction>> map = new HashMap<>();
        for (Transaction tx : txList) {
            List<Transaction> list = map.get(tx.getBlockHeight());
            if (null == list) {
                list = new ArrayList<>();
            }
            list.add(tx);
            map.put(tx.getBlockHeight(), list);
        }
        return map;
    }

    public BlockHeader getBlockHeader(long height) throws NulsException {
        BlockHeader header = blockCacheManager.getBlockHeader(height);
        if (null != header) {
            return header;
        }
        Block block = blockCacheManager.getBlock(height);
        if (null != block) {
            header = block.getHeader();
            return header;
        }
        BlockHeaderPo po = this.headerDao.getHeader(height);
        return ConsensusTool.fromPojo(po);
    }

    public BlockHeader getBlockHeader(String hash) throws NulsException {
        BlockHeader header = blockCacheManager.getBlockHeader(hash);
        if (null != header) {
            return header;
        }
        Block block = blockCacheManager.getBlock(hash);
        if (null != block) {
            return block.getHeader();
        }
        BlockHeaderPo po = this.headerDao.getHeader(hash);
        return ConsensusTool.fromPojo(po);
    }

    public long getBestHeight() {
        return headerDao.getBestHeight();
    }

    public void save(Block block) {
        BlockHeader header = block.getHeader();
        header.setSize(block.size());
        headerDao.save(ConsensusTool.toPojo(header));
    }

    public void delete(String hash) {
        blockCacheManager.removeBlock(hash);
        headerDao.delete(hash);
    }

    public List<BlockHeader> getBlockHeaderList(long startHeight, long endHeight, long split) {
        List<BlockHeaderPo> strList = this.headerDao.getHashList(startHeight, endHeight, split);
        Map<Long, BlockHeader> headerMap = new HashMap<>();
        for (BlockHeaderPo po : strList) {
            BlockHeader header = new BlockHeader();
            header.setHash(NulsDigestData.fromDigestHex(po.getHash()));
            header.setHeight(po.getHeight());
            headerMap.put(po.getHeight(), header);
        }
        if ((endHeight - startHeight + 1) == headerMap.size()) {
            return new ArrayList<>(headerMap.values());
        }
        List<BlockHeader> headerList = new ArrayList<>();
        for (long i = startHeight; i <= endHeight; i++) {
            if (headerMap.containsKey(i)) {
                headerList.add(headerMap.get(i));
                continue;
            }
            BlockHeader header = blockCacheManager.getBlockHeader(i);
            if (null == header) {
                Block block = blockCacheManager.getBlock(i);
                if (null != block) {
                    header = block.getHeader();
                }
            }
            if (null != header) {
                headerList.add(header);
            }
        }
        return headerList;
    }

    public Page<BlockHeaderPo> getBlocListByAddress(String nodeAddress, int type, int pageNumber, int pageSize) {
        return headerDao.getBlockListByAddress(nodeAddress, type, pageNumber, pageSize);
    }

    public Page<BlockHeaderPo> getBlockHeaderList(int pageNumber, int pageSize) {
        return headerDao.getBlockHeaderList(pageNumber, pageSize);
    }


    public long getBlockCount(String address, long roundStart, long roundEnd) {
        return this.headerDao.getCount(address, roundStart, roundEnd);
    }

    public long getSumOfRoundIndexOfYellowPunish(String address, long startRoundIndex, long endRoundIndex) {
        //todo 是否需要查询内存
        List<Long> indexList = this.headerDao.getListOfRoundIndexOfYellowPunish(address, startRoundIndex, endRoundIndex);
        if (null == indexList || indexList.isEmpty()) {
            return 0L;
        }
        long value = 0;
        for (Long index : indexList) {
            value += (index - startRoundIndex + 1);
        }
        return value;
    }
}
